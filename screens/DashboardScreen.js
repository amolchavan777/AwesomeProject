import React, { useState, useEffect } from 'react';
import { View, Text, ScrollView, StyleSheet, RefreshControl, TouchableOpacity, ActivityIndicator } from 'react-native';

const API_BASE_URL = 'http://localhost:8082/api/analytics';

export default function DashboardScreen() {
  const [data, setData] = useState({
    summary: null,
    criticality: null,
    health: null,
    serviceMesh: null
  });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(new Date());

  const fetchAllData = async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);

    try {
      const [summaryRes, criticalityRes, healthRes, serviceMeshRes] = await Promise.all([
        fetch(`${API_BASE_URL}/summary`),
        fetch(`${API_BASE_URL}/criticality`),
        fetch(`${API_BASE_URL}/health`),
        fetch(`${API_BASE_URL}/service-mesh`)
      ]);

      if (!summaryRes.ok || !criticalityRes.ok || !healthRes.ok || !serviceMeshRes.ok) {
        throw new Error('Failed to fetch dashboard data');
      }

      const [summary, criticality, health, serviceMesh] = await Promise.all([
        summaryRes.json(),
        criticalityRes.json(),
        healthRes.json(),
        serviceMeshRes.json()
      ]);

      setData({ summary, criticality, health, serviceMesh });
      setLastUpdate(new Date());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchAllData();
  }, []);

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#3498db" />
        <Text style={styles.loadingText}>Loading Dashboard...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centerContainer}>
        <Text style={styles.errorText}>Error: {error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={() => fetchAllData()}>
          <Text style={styles.retryButtonText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const getHealthColor = (health) => {
    if (health >= 0.8) return '#27ae60';
    if (health >= 0.6) return '#f39c12';
    return '#e74c3c';
  };

  const getRiskColor = (risk) => {
    if (risk >= 0.8) return '#e74c3c';
    if (risk >= 0.5) return '#f39c12';
    return '#27ae60';
  };

  return (
    <ScrollView 
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={() => fetchAllData(true)} />
      }
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Dependency Matrix Dashboard</Text>
        <Text style={styles.subtitle}>
          Last updated: {lastUpdate.toLocaleTimeString()}
        </Text>
      </View>

      {/* Key Metrics Row */}
      <View style={styles.metricsRow}>
        <View style={styles.metricCard}>
          <Text style={styles.metricNumber}>
            {data.summary?.claimsBySource ? Object.values(data.summary.claimsBySource).reduce((a, b) => a + b, 0) : 0}
          </Text>
          <Text style={styles.metricLabel}>Total Dependencies</Text>
        </View>
        
        <View style={styles.metricCard}>
          <Text style={[styles.metricNumber, { color: getHealthColor(data.health?.averageHealth || 0) }]}>
            {((data.health?.averageHealth || 0) * 100).toFixed(0)}%
          </Text>
          <Text style={styles.metricLabel}>System Health</Text>
        </View>
        
        <View style={styles.metricCard}>
          <Text style={[styles.metricNumber, { color: getRiskColor(data.serviceMesh?.cascadeFailure?.riskScore || 0) }]}>
            {((data.serviceMesh?.cascadeFailure?.riskScore || 0) * 100).toFixed(0)}%
          </Text>
          <Text style={styles.metricLabel}>Risk Score</Text>
        </View>
      </View>

      {/* Service Mesh Traffic */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🚦 Top Traffic Routes</Text>
        {data.serviceMesh?.trafficAnalysis?.topTrafficRoutes?.slice(0, 3).map((route, index) => (
          <View key={index} style={styles.trafficRoute}>
            <Text style={styles.routePath}>{route.source} → {route.destination}</Text>
            <View style={styles.routeMetrics}>
              <Text style={styles.routeCount}>{route.requestCount} req/min</Text>
              <Text style={styles.routeLatency}>{route.averageLatency}ms avg</Text>
            </View>
          </View>
        ))}
      </View>

      {/* Critical Services */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>⚠️ Critical Services</Text>
        {data.criticality?.topCriticalServices?.slice(0, 3).map((service, index) => (
          <View key={index} style={styles.criticalService}>
            <Text style={styles.serviceName}>{service.service}</Text>
            <Text style={styles.criticalityScore}>
              Score: {service.criticalityScore.toFixed(2)}
            </Text>
          </View>
        ))}
      </View>

      {/* Performance Issues */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🔥 Performance Hotspots</Text>
        {data.serviceMesh?.trafficAnalysis?.latencyHotspots?.map((hotspot, index) => (
          <View key={index} style={styles.hotspot}>
            <Text style={styles.hotspotService}>{hotspot.service}</Text>
            <Text style={styles.hotspotLatency}>P99: {hotspot.p99Latency}ms</Text>
            <Text style={styles.hotspotDescription}>{hotspot.description}</Text>
          </View>
        ))}
      </View>

      {/* Error Rates */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🚨 Error Rates</Text>
        <Text style={styles.overallError}>
          Overall: {data.serviceMesh?.trafficAnalysis?.errorRateAnalysis?.overallErrorRate}%
        </Text>
        {data.serviceMesh?.trafficAnalysis?.errorRateAnalysis?.serviceErrors?.map((error, index) => (
          <View key={index} style={styles.serviceError}>
            <Text style={styles.errorService}>{error.service}</Text>
            <Text style={[styles.errorRate, { color: error.errorRate > 3 ? '#e74c3c' : '#f39c12' }]}>
              {error.errorRate}%
            </Text>
          </View>
        ))}
      </View>

      {/* Cascade Failure Risks */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>⚡ Cascade Failure Risks</Text>
        {data.serviceMesh?.cascadeFailure?.criticalPaths?.map((path, index) => (
          <View key={index} style={styles.criticalPath}>
            <Text style={styles.pathText}>{path.path}</Text>
            <Text style={[styles.pathProbability, { 
              color: path.probability > 0.9 ? '#e74c3c' : path.probability > 0.8 ? '#f39c12' : '#27ae60' 
            }]}>
              {(path.probability * 100).toFixed(0)}% risk
            </Text>
          </View>
        ))}
      </View>

      {/* Service Discovery */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🔍 Service Discovery</Text>
        {data.serviceMesh?.serviceDiscovery?.newlyDiscoveredServices?.length > 0 && (
          <View style={styles.discoveryItem}>
            <Text style={styles.discoveryLabel}>New Services:</Text>
            <Text style={styles.discoveryValue}>
              {data.serviceMesh.serviceDiscovery.newlyDiscoveredServices.join(', ')}
            </Text>
          </View>
        )}
        {data.serviceMesh?.serviceDiscovery?.orphanedServices?.length > 0 && (
          <View style={styles.discoveryItem}>
            <Text style={styles.discoveryLabel}>Orphaned:</Text>
            <Text style={[styles.discoveryValue, { color: '#e74c3c' }]}>
              {data.serviceMesh.serviceDiscovery.orphanedServices.join(', ')}
            </Text>
          </View>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
  },
  header: {
    padding: 20,
    backgroundColor: '#2c3e50',
    borderBottomWidth: 1,
    borderBottomColor: '#34495e',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    color: '#bdc3c7',
  },
  metricsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 16,
    backgroundColor: '#fff',
    marginBottom: 8,
  },
  metricCard: {
    alignItems: 'center',
    flex: 1,
  },
  metricNumber: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  metricLabel: {
    fontSize: 12,
    color: '#7f8c8d',
    textAlign: 'center',
    marginTop: 4,
  },
  section: {
    backgroundColor: '#fff',
    marginBottom: 8,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 12,
  },
  trafficRoute: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  routePath: {
    fontSize: 14,
    fontWeight: '500',
    color: '#34495e',
    flex: 1,
  },
  routeMetrics: {
    flexDirection: 'row',
    gap: 12,
  },
  routeCount: {
    fontSize: 12,
    color: '#3498db',
    fontWeight: '500',
  },
  routeLatency: {
    fontSize: 12,
    color: '#e67e22',
    fontWeight: '500',
  },
  criticalService: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  serviceName: {
    fontSize: 14,
    fontWeight: '500',
    color: '#34495e',
  },
  criticalityScore: {
    fontSize: 12,
    color: '#e74c3c',
    fontWeight: '500',
  },
  hotspot: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  hotspotService: {
    fontSize: 14,
    fontWeight: '600',
    color: '#e74c3c',
    marginBottom: 2,
  },
  hotspotLatency: {
    fontSize: 12,
    color: '#f39c12',
    fontWeight: '500',
    marginBottom: 2,
  },
  hotspotDescription: {
    fontSize: 11,
    color: '#7f8c8d',
    fontStyle: 'italic',
  },
  overallError: {
    fontSize: 14,
    fontWeight: '600',
    color: '#e74c3c',
    marginBottom: 8,
  },
  serviceError: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 4,
  },
  errorService: {
    fontSize: 12,
    color: '#34495e',
  },
  errorRate: {
    fontSize: 12,
    fontWeight: '600',
  },
  criticalPath: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  pathText: {
    fontSize: 12,
    color: '#34495e',
    flex: 1,
    marginRight: 8,
  },
  pathProbability: {
    fontSize: 12,
    fontWeight: '600',
  },
  discoveryItem: {
    marginBottom: 8,
  },
  discoveryLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#34495e',
    marginBottom: 2,
  },
  discoveryValue: {
    fontSize: 11,
    color: '#7f8c8d',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#7f8c8d',
  },
  errorText: {
    fontSize: 16,
    color: '#e74c3c',
    textAlign: 'center',
    marginBottom: 16,
  },
  retryButton: {
    backgroundColor: '#3498db',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 6,
  },
  retryButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
});

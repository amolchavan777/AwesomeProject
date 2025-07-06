import React from 'react';
import { View, Text, ScrollView, ActivityIndicator, StyleSheet, TouchableOpacity } from 'react-native';

const API_BASE_URL = 'http://localhost:8082/api/analytics';

export default class ServiceMeshScreen extends React.Component {
  static navigationOptions = {
    title: 'Service Mesh Analytics',
  };

  state = {
    meshData: null,
    loading: true,
    error: null,
    selectedTab: 'traffic',
  };

  componentDidMount() {
    this.loadServiceMeshData();
  }

  loadServiceMeshData = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/service-mesh`);
      
      if (!response.ok) {
        throw new Error(`Failed to fetch service mesh data: ${response.status}`);
      }

      const meshData = await response.json();

      this.setState({
        meshData,
        loading: false,
      });
    } catch (err) {
      this.setState({
        error: err.message,
        loading: false,
      });
    }
  };

  renderTrafficAnalysis = () => {
    const { meshData } = this.state;
    if (!meshData || !meshData.trafficAnalysis) return <Text>No traffic data available</Text>;

    const trafficData = meshData.trafficAnalysis;

    return (
      <View>
        <Text style={styles.section}>🚦 Traffic Analysis</Text>
        
        <Text style={styles.subsection}>Top Traffic Routes</Text>
        {trafficData.topTrafficRoutes && trafficData.topTrafficRoutes.map((route, index) => (
          <View key={index} style={styles.routeCard}>
            <Text style={styles.routeText}>{route.source} → {route.destination}</Text>
            <Text style={styles.routeMetrics}>
              {route.requestsPerMinute} req/min • {route.averageLatencyMs.toFixed(1)}ms avg
            </Text>
          </View>
        ))}

        <Text style={styles.subsection}>🔥 Latency Hotspots</Text>
        {trafficData.latencyHotspots && trafficData.latencyHotspots.map((hotspot, index) => (
          <View key={index} style={styles.hotspotCard}>
            <Text style={styles.hotspotText}>{hotspot.source} → {hotspot.destination}</Text>
            <Text style={styles.hotspotLatency}>{hotspot.latencyMs.toFixed(1)}ms</Text>
            <Text style={styles.hotspotIssue}>{hotspot.issue}</Text>
          </View>
        ))}

        <Text style={styles.subsection}>📊 Throughput Metrics</Text>
        {trafficData.throughputMetrics && (
          <View style={styles.metricsCard}>
            <Text style={styles.metricText}>Peak RPS: {trafficData.throughputMetrics.peakRPS}</Text>
            <Text style={styles.metricText}>Average RPS: {trafficData.throughputMetrics.averageRPS}</Text>
          </View>
        )}
      </View>
    );
  };

  renderCascadePrediction = () => {
    const { meshData } = this.state;
    if (!meshData || !meshData.cascadePrediction) return <Text>No cascade prediction data available</Text>;

    const cascadePrediction = meshData.cascadePrediction;

    return (
      <View>
        <Text style={styles.section}>⚠️ Cascade Failure Prediction</Text>
        
        <View style={styles.riskCard}>
          <Text style={styles.riskScore}>Risk Score: {cascadePrediction.riskScore?.toFixed(1)}/10</Text>
        </View>

        <Text style={styles.subsection}>Critical Paths</Text>
        {cascadePrediction.criticalPaths && cascadePrediction.criticalPaths.map((path, index) => (
          <View key={index} style={styles.pathCard}>
            <Text style={styles.pathText}>{path.services.join(' → ')}</Text>
            <Text style={styles.pathRisk}>Risk: {path.riskScore.toFixed(1)}/10</Text>
          </View>
        ))}

        <Text style={styles.subsection}>Failure Impact Analysis</Text>
        {cascadePrediction.failureImpactMap && Object.entries(cascadePrediction.failureImpactMap).slice(0, 5).map(([service, impact], index) => (
          <View key={index} style={styles.impactCard}>
            <Text style={styles.impactService}>{service}</Text>
            <Text style={styles.impactBusiness}>{impact.businessImpact}</Text>
            <Text style={styles.impactRecovery}>Recovery: {impact.recoveryTime}min</Text>
            <Text style={styles.impactAffected}>
              Affects {impact.affectedServices?.size || 0} services
            </Text>
          </View>
        ))}
      </View>
    );
  };

  renderServiceDiscovery = () => {
    const { meshData } = this.state;
    if (!meshData || !meshData.serviceDiscovery) return <Text>No service discovery data available</Text>;

    const serviceDiscovery = meshData.serviceDiscovery;

    return (
      <View>
        <Text style={styles.section}>🔍 Dynamic Service Discovery</Text>
        
        <Text style={styles.subsection}>✅ Newly Discovered Services</Text>
        {serviceDiscovery.newlyDiscoveredServices && serviceDiscovery.newlyDiscoveredServices.map((service, index) => (
          <Text key={index} style={styles.newService}>+ {service}</Text>
        ))}

        <Text style={styles.subsection}>⚠️ Orphaned Services</Text>
        {serviceDiscovery.orphanedServices && serviceDiscovery.orphanedServices.map((service, index) => (
          <Text key={index} style={styles.orphanedService}>- {service}</Text>
        ))}

        <Text style={styles.subsection}>📋 Service Versions</Text>
        {serviceDiscovery.serviceVersions && Object.entries(serviceDiscovery.serviceVersions).map(([service, version], index) => (
          <View key={index} style={styles.versionCard}>
            <Text style={styles.versionService}>{service}</Text>
            <Text style={styles.versionNumber}>{version}</Text>
          </View>
        ))}

        {serviceDiscovery.discoveryTimestamp && (
          <Text style={styles.timestamp}>
            Last updated: {new Date(serviceDiscovery.discoveryTimestamp).toLocaleString()}
          </Text>
        )}
      </View>
    );
  };

  renderPerformanceCorrelation = () => {
    const { meshData } = this.state;
    if (!meshData || !meshData.performanceCorrelation) return <Text>No performance correlation data available</Text>;

    const performanceCorrelation = meshData.performanceCorrelation;

    return (
      <View>
        <Text style={styles.section}>📈 Performance Correlation Analysis</Text>
        
        <Text style={styles.subsection}>Service Correlations</Text>
        {performanceCorrelation.correlatedServices && performanceCorrelation.correlatedServices.map((correlation, index) => (
          <View key={index} style={styles.correlationCard}>
            <Text style={styles.correlationPair}>
              {correlation.service1} ↔ {correlation.service2}
            </Text>
            <Text style={styles.correlationCoeff}>
              Correlation: {(correlation.correlationCoefficient * 100).toFixed(1)}%
            </Text>
            <Text style={styles.correlationDesc}>{correlation.description}</Text>
          </View>
        ))}

        <Text style={styles.subsection}>🌊 Latency Propagation</Text>
        {performanceCorrelation.latencyPropagation && performanceCorrelation.latencyPropagation.map((prop, index) => (
          <View key={index} style={styles.propagationCard}>
            <Text style={styles.propagationSource}>{prop.sourceService}</Text>
            <Text style={styles.propagationAffected}>
              Affects: {prop.affectedServices.join(', ')}
            </Text>
            <Text style={styles.propagationFactor}>
              Propagation Factor: {prop.propagationFactor.toFixed(1)}x
            </Text>
          </View>
        ))}

        <Text style={styles.subsection}>🚫 Resource Bottlenecks</Text>
        {performanceCorrelation.resourceBottlenecks && performanceCorrelation.resourceBottlenecks.map((bottleneck, index) => (
          <View key={index} style={styles.bottleneckCard}>
            <Text style={styles.bottleneckService}>{bottleneck.service}</Text>
            <Text style={styles.bottleneckResource}>
              {bottleneck.resourceType}: {bottleneck.utilizationPercent.toFixed(1)}%
            </Text>
            <Text style={styles.bottleneckDesc}>{bottleneck.description}</Text>
          </View>
        ))}
      </View>
    );
  };

  render() {
    const { loading, error, selectedTab } = this.state;

    if (loading) return <ActivityIndicator size="large" style={{ marginTop: 40 }} />;
    if (error) return <Text style={styles.error}>Error: {error}</Text>;

    let currentContent;
    switch (selectedTab) {
      case 'cascade':
        currentContent = this.renderCascadePrediction();
        break;
      case 'discovery':
        currentContent = this.renderServiceDiscovery();
        break;
      case 'performance':
        currentContent = this.renderPerformanceCorrelation();
        break;
      default:
        currentContent = this.renderTrafficAnalysis();
    }

    return (
      <View style={styles.container}>
        {/* Tab Navigation */}
        <ScrollView horizontal style={styles.tabContainer} showsHorizontalScrollIndicator={false}>
          {[
            { key: 'traffic', label: '🚦 Traffic' },
            { key: 'cascade', label: '⚠️ Cascade' },
            { key: 'discovery', label: '🔍 Discovery' },
            { key: 'performance', label: '📈 Performance' },
          ].map((tab) => (
            <TouchableOpacity
              key={tab.key}
              style={[styles.tab, selectedTab === tab.key && styles.activeTab]}
              onPress={() => this.setState({ selectedTab: tab.key })}
            >
              <Text style={[styles.tabText, selectedTab === tab.key && styles.activeTabText]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Content */}
        <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
          {currentContent}
        </ScrollView>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  tabContainer: {
    flexGrow: 0,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
    paddingVertical: 10,
  },
  tab: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    marginHorizontal: 4,
    borderRadius: 16,
    backgroundColor: '#f8f9fa',
  },
  activeTab: {
    backgroundColor: '#007bff',
  },
  tabText: {
    fontSize: 13,
    color: '#6c757d',
    fontWeight: '600',
  },
  activeTabText: {
    color: '#fff',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  section: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    color: '#212529',
  },
  subsection: {
    fontSize: 16,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
    color: '#495057',
  },
  routeCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#28a745',
  },
  routeText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212529',
  },
  routeMetrics: {
    fontSize: 12,
    color: '#6c757d',
    marginTop: 4,
  },
  hotspotCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#dc3545',
  },
  hotspotText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212529',
  },
  hotspotLatency: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#dc3545',
  },
  hotspotIssue: {
    fontSize: 12,
    color: '#6c757d',
    marginTop: 4,
  },
  metricsCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    marginVertical: 8,
  },
  metricText: {
    fontSize: 14,
    color: '#495057',
    marginVertical: 2,
  },
  riskCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 16,
    borderWidth: 2,
    borderColor: '#ffc107',
  },
  riskScore: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#856404',
  },
  pathCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#ffc107',
  },
  pathText: {
    fontSize: 14,
    color: '#212529',
  },
  pathRisk: {
    fontSize: 12,
    fontWeight: '600',
    color: '#856404',
    marginTop: 4,
  },
  impactCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#17a2b8',
  },
  impactService: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212529',
  },
  impactBusiness: {
    fontSize: 12,
    color: '#dc3545',
    fontWeight: '600',
  },
  impactRecovery: {
    fontSize: 12,
    color: '#6c757d',
  },
  impactAffected: {
    fontSize: 12,
    color: '#6c757d',
  },
  newService: {
    fontSize: 14,
    color: '#28a745',
    fontWeight: '600',
    paddingVertical: 4,
  },
  orphanedService: {
    fontSize: 14,
    color: '#dc3545',
    fontWeight: '600',
    paddingVertical: 4,
  },
  versionCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  versionService: {
    fontSize: 14,
    color: '#212529',
  },
  versionNumber: {
    fontSize: 14,
    fontWeight: '600',
    color: '#007bff',
  },
  timestamp: {
    fontSize: 12,
    color: '#6c757d',
    textAlign: 'center',
    marginTop: 16,
  },
  correlationCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#6f42c1',
  },
  correlationPair: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212529',
  },
  correlationCoeff: {
    fontSize: 14,
    fontWeight: '600',
    color: '#6f42c1',
  },
  correlationDesc: {
    fontSize: 12,
    color: '#6c757d',
  },
  propagationCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#fd7e14',
  },
  propagationSource: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212529',
  },
  propagationAffected: {
    fontSize: 12,
    color: '#6c757d',
  },
  propagationFactor: {
    fontSize: 12,
    fontWeight: '600',
    color: '#fd7e14',
  },
  bottleneckCard: {
    backgroundColor: '#fff',
    padding: 12,
    marginVertical: 4,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#e83e8c',
  },
  bottleneckService: {
    fontSize: 14,
    fontWeight: '600',
    color: '#212529',
  },
  bottleneckResource: {
    fontSize: 14,
    fontWeight: '600',
    color: '#e83e8c',
  },
  bottleneckDesc: {
    fontSize: 12,
    color: '#6c757d',
  },
  error: {
    color: '#dc3545',
    fontSize: 16,
    textAlign: 'center',
    margin: 20,
  },
});

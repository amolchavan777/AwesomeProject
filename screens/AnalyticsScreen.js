import React, { useEffect, useState } from 'react';
import { View, Text, ScrollView, ActivityIndicator, StyleSheet, TouchableOpacity } from 'react-native';
import ImpactAnalysisModal from '../components/ImpactAnalysisModal';

const API_BASE_URL = 'http://localhost:8082/api/analytics';

export default function AnalyticsScreen() {
  const [analytics, setAnalytics] = useState(null);
  const [criticality, setCriticality] = useState(null);
  const [topology, setTopology] = useState(null);
  const [bottlenecks, setBottlenecks] = useState(null);
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedTab, setSelectedTab] = useState('basic');
  const [showImpactModal, setShowImpactModal] = useState(false);

  useEffect(() => {
    Promise.all([
      fetch(`${API_BASE_URL}/summary`),
      fetch(`${API_BASE_URL}/criticality`),
      fetch(`${API_BASE_URL}/topology`),
      fetch(`${API_BASE_URL}/bottlenecks`),
      fetch(`${API_BASE_URL}/health`)
    ])
      .then(responses => {
        const failedResponse = responses.find(res => !res.ok);
        if (failedResponse) {
          throw new Error(`Failed to fetch analytics: ${failedResponse.status}`);
        }
        return Promise.all(responses.map(res => res.json()));
      })
      .then(([summaryData, criticalityData, topologyData, bottlenecksData, healthData]) => {
        setAnalytics(summaryData);
        setCriticality(criticalityData);
        setTopology(topologyData);
        setBottlenecks(bottlenecksData);
        setHealth(healthData);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  if (loading) return <ActivityIndicator size="large" style={{ marginTop: 40 }} />;
  if (error) return <Text style={styles.error}>Error: {error}</Text>;

  const renderBasicAnalytics = () => (
    <View>
      <Text style={styles.section}>Top 5 Most Common Dependencies</Text>
      {analytics?.topDependencies?.map((dep) => (
        <Text key={dep.dependency} style={styles.item}>{dep.dependency} ({dep.count} claims)</Text>
      ))}

      <Text style={styles.section}>Top 3 Most Connected (Outgoing)</Text>
      {analytics?.topOutDegree?.map((svc) => (
        <Text key={svc.service} style={styles.item}>{svc.service} ({svc.outgoing} outgoing)</Text>
      ))}

      <Text style={styles.section}>Top 3 Most Connected (Incoming)</Text>
      {analytics?.topInDegree?.map((svc) => (
        <Text key={svc.service} style={styles.item}>{svc.service} ({svc.incoming} incoming)</Text>
      ))}

      <Text style={styles.section}>Average Confidence</Text>
      <Text style={styles.item}>{(analytics?.averageConfidence * 100)?.toFixed(1)}%</Text>
    </View>
  );

  const renderCriticalityAnalysis = () => (
    <View>
      <Text style={styles.section}>Top 10 Most Critical Services</Text>
      {criticality?.topCriticalServices?.map((svc) => (
        <Text key={svc.service} style={styles.item}>
          {svc.service} (Score: {svc.criticalityScore.toFixed(3)})
        </Text>
      ))}
    </View>
  );

  const renderTopologyMetrics = () => (
    <View>
      <Text style={styles.section}>Network Topology Metrics</Text>
      <Text style={styles.item}>Network Density: {(topology?.networkDensity * 100)?.toFixed(1)}%</Text>
      <Text style={styles.item}>Network Diameter: {topology?.networkDiameter}</Text>
      <Text style={styles.item}>Average Clustering: {(topology?.averageClustering * 100)?.toFixed(1)}%</Text>
      
      <Text style={styles.section}>Top Services by Betweenness Centrality</Text>
      {Object.entries(topology?.betweennessCentrality || {})
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
        .map(([service, centrality]) => (
          <Text key={service} style={styles.item}>
            {service} ({centrality.toFixed(3)})
          </Text>
        ))}
    </View>
  );

  const renderBottlenecks = () => (
    <View>
      <Text style={styles.section}>Identified Bottlenecks ({bottlenecks?.bottleneckCount || 0})</Text>
      {bottlenecks?.bottleneckServices?.map((bottleneck) => (
        <View key={bottleneck.service} style={styles.bottleneckItem}>
          <Text style={styles.bottleneckService}>{bottleneck.service}</Text>
          <Text style={[styles.riskBadge, styles[`risk${bottleneck.risk}`]]}>{bottleneck.risk}</Text>
          <Text style={styles.item}>In-degree: {bottleneck.inDegree}, Out-degree: {bottleneck.outDegree}</Text>
          <Text style={styles.item}>Centrality: {bottleneck.betweennessCentrality.toFixed(3)}</Text>
          <Text style={styles.reason}>{bottleneck.reason}</Text>
        </View>
      ))}
      {(!bottlenecks?.bottleneckServices || bottlenecks.bottleneckServices.length === 0) && (
        <Text style={styles.item}>No bottlenecks detected</Text>
      )}
    </View>
  );

  const renderHealthScores = () => (
    <View>
      <Text style={styles.section}>Dependency Health Overview</Text>
      <Text style={styles.item}>Average Health: {(health?.averageHealth * 100)?.toFixed(1)}%</Text>
      <Text style={styles.item}>Total Dependencies: {health?.totalDependencies}</Text>
      <Text style={styles.item}>Unhealthy Dependencies: {health?.unhealthyDependencies}</Text>
      
      <Text style={styles.section}>Health Scores by Dependency</Text>
      {Object.entries(health?.healthScores || {})
        .sort((a, b) => a[1] - b[1]) // Sort by health score (worst first)
        .slice(0, 8) // Show top 8 worst/best
        .map(([dependency, score]) => {
          let healthColor = '#27ae60'; // Default green
          if (score < 0.5) {
            healthColor = '#e74c3c'; // Red
          } else if (score < 0.7) {
            healthColor = '#f39c12'; // Orange
          }
          return (
            <Text key={dependency} style={[styles.item, { color: healthColor }]}>
              {dependency} ({(score * 100).toFixed(1)}%)
            </Text>
          );
        })}
    </View>
  );

  const renderCurrentTab = () => {
    switch (selectedTab) {
      case 'basic': return renderBasicAnalytics();
      case 'criticality': return renderCriticalityAnalysis();
      case 'topology': return renderTopologyMetrics();
      case 'bottlenecks': return renderBottlenecks();
      case 'health': return renderHealthScores();
      default: return renderBasicAnalytics();
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Advanced Analytics Dashboard</Text>
      
      {/* Tab Navigation */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabContainer}>
        {[
          { key: 'basic', label: 'Basic' },
          { key: 'criticality', label: 'Criticality' },
          { key: 'topology', label: 'Topology' },
          { key: 'bottlenecks', label: 'Bottlenecks' },
          { key: 'health', label: 'Health' }
        ].map(tab => (
          <TouchableOpacity
            key={tab.key}
            style={[styles.tab, selectedTab === tab.key && styles.activeTab]}
            onPress={() => setSelectedTab(tab.key)}
          >
            <Text style={[styles.tabText, selectedTab === tab.key && styles.activeTabText]}>
              {tab.label}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/* Content */}
      <ScrollView style={styles.content}>
        {renderCurrentTab()}
      </ScrollView>

      {/* Impact Analysis Modal */}
      <ImpactAnalysisModal 
        visible={showImpactModal} 
        onClose={() => setShowImpactModal(false)} 
      />

      {/* Floating Action Button for Impact Analysis */}
      <TouchableOpacity 
        style={styles.fab} 
        onPress={() => setShowImpactModal(true)}
      >
        <Text style={styles.fabText}>🎯</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { 
    flex: 1, 
    backgroundColor: '#f8f9fa' 
  },
  header: { 
    fontSize: 24, 
    fontWeight: 'bold', 
    marginBottom: 16,
    paddingHorizontal: 16,
    paddingTop: 16,
    color: '#2c3e50'
  },
  tabContainer: {
    paddingHorizontal: 16,
    marginBottom: 16,
    maxHeight: 50
  },
  tab: {
    paddingHorizontal: 20,
    paddingVertical: 8,
    marginRight: 10,
    backgroundColor: '#ecf0f1',
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center'
  },
  activeTab: {
    backgroundColor: '#3498db'
  },
  tabText: {
    fontSize: 14,
    color: '#7f8c8d',
    fontWeight: '500'
  },
  activeTabText: {
    color: '#fff',
    fontWeight: '600'
  },
  content: {
    flex: 1,
    paddingHorizontal: 16
  },
  section: { 
    fontSize: 18, 
    fontWeight: '600', 
    marginTop: 20, 
    marginBottom: 10,
    color: '#34495e'
  },
  item: { 
    fontSize: 14, 
    marginBottom: 8, 
    paddingLeft: 12,
    color: '#5d6d7e'
  },
  bottleneckItem: {
    backgroundColor: '#fff',
    padding: 12,
    marginBottom: 10,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#e74c3c'
  },
  bottleneckService: {
    fontSize: 16,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 4
  },
  riskBadge: {
    fontSize: 12,
    fontWeight: '700',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
    marginBottom: 8,
    textTransform: 'uppercase'
  },
  riskHIGH: {
    backgroundColor: '#e74c3c',
    color: '#fff'
  },
  riskMEDIUM: {
    backgroundColor: '#f39c12',
    color: '#fff'
  },
  riskLOW: {
    backgroundColor: '#27ae60',
    color: '#fff'
  },
  reason: {
    fontSize: 12,
    fontStyle: 'italic',
    color: '#7f8c8d',
    marginTop: 4
  },
  error: { 
    color: '#e74c3c', 
    textAlign: 'center', 
    marginTop: 40,
    fontSize: 16
  },
  fab: {
    position: 'absolute',
    bottom: 30,
    right: 30,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#3498db',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5
  },
  fabText: {
    fontSize: 24
  }
});

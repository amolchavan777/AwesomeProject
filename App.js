import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert } from 'react-native';

export default function App() {
  const showBackendInfo = () => {
    Alert.alert(
      "Backend Information", 
      "Backend API is running on http://localhost:8082\n\nKey endpoints:\n• /api/analytics/service-mesh\n• /api/analytics/traffic-analysis\n• /api/ingest/observability",
      [{ text: "OK" }]
    );
  };

  return (
    <View style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>🔗 Dependency Matrix</Text>
        <Text style={styles.subtitle}>Application Dependency Analyzer</Text>
        
        <Text style={styles.description}>
          Advanced microservice dependency mapping with real-time analytics, 
          cascade failure detection, and multi-source data ingestion.
        </Text>

        <TouchableOpacity style={styles.button} onPress={showBackendInfo}>
          <Text style={styles.buttonText}>📊 View API Endpoints</Text>
        </TouchableOpacity>

        <View style={styles.featuresContainer}>
          <Text style={styles.featuresTitle}>✨ Key Features</Text>
          
          <View style={styles.feature}>
            <Text style={styles.featureIcon}>🔍</Text>
            <Text style={styles.featureText}>Service Mesh Analytics</Text>
          </View>
          
          <View style={styles.feature}>
            <Text style={styles.featureIcon}>⚡</Text>
            <Text style={styles.featureText}>Traffic Flow Analysis</Text>
          </View>
          
          <View style={styles.feature}>
            <Text style={styles.featureIcon}>🔗</Text>
            <Text style={styles.featureText}>Cascade Failure Detection</Text>
          </View>
          
          <View style={styles.feature}>
            <Text style={styles.featureIcon}>📡</Text>
            <Text style={styles.featureText}>Multi-Source Ingestion</Text>
          </View>
          
          <View style={styles.feature}>
            <Text style={styles.featureIcon}>☸️</Text>
            <Text style={styles.featureText}>Kubernetes Integration</Text>
          </View>
        </View>

        <View style={styles.statusContainer}>
          <Text style={styles.statusTitle}>System Status</Text>
          <View style={styles.statusRow}>
            <Text style={styles.statusLabel}>Frontend:</Text>
            <Text style={[styles.statusValue, styles.online]}>ONLINE</Text>
          </View>
          <View style={styles.statusRow}>
            <Text style={styles.statusLabel}>Backend API:</Text>
            <Text style={[styles.statusValue, styles.online]}>PORT 8082</Text>
          </View>
          <View style={styles.statusRow}>
            <Text style={styles.statusLabel}>Database:</Text>
            <Text style={[styles.statusValue, styles.online]}>H2 READY</Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollContent: {
    padding: 20,
    alignItems: 'center',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 8,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 18,
    color: '#7f8c8d',
    marginBottom: 20,
    textAlign: 'center',
  },
  description: {
    fontSize: 16,
    color: '#34495e',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 30,
    paddingHorizontal: 10,
  },
  button: {
    backgroundColor: '#3498db',
    paddingHorizontal: 30,
    paddingVertical: 15,
    borderRadius: 25,
    marginBottom: 30,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  featuresContainer: {
    width: '100%',
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  featuresTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 15,
    textAlign: 'center',
  },
  feature: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  featureIcon: {
    fontSize: 20,
    marginRight: 12,
  },
  featureText: {
    fontSize: 16,
    color: '#34495e',
  },
  statusContainer: {
    width: '100%',
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statusTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 15,
    textAlign: 'center',
  },
  statusRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  statusLabel: {
    fontSize: 16,
    color: '#7f8c8d',
  },
  statusValue: {
    fontSize: 16,
    fontWeight: '600',
  },
  online: {
    color: '#27ae60',
  },
});

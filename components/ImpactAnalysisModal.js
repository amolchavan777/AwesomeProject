import React, { useState } from 'react';
import { 
  Modal, 
  View, 
  Text, 
  TextInput, 
  TouchableOpacity, 
  ScrollView, 
  StyleSheet, 
  ActivityIndicator 
} from 'react-native';

const API_BASE_URL = 'http://localhost:8082/api/analytics';

export default function ImpactAnalysisModal({ visible, onClose }) {
  const [serviceName, setServiceName] = useState('');
  const [impactData, setImpactData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const analyzeImpact = async () => {
    if (!serviceName.trim()) {
      setError('Please enter a service name');
      return;
    }

    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`${API_BASE_URL}/impact-analysis?serviceName=${encodeURIComponent(serviceName.trim())}`);
      if (!response.ok) {
        throw new Error(`Failed to analyze impact: ${response.status}`);
      }
      const data = await response.json();
      setImpactData(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const reset = () => {
    setServiceName('');
    setImpactData(null);
    setError(null);
    setLoading(false);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      presentationStyle="pageSheet"
      onRequestClose={handleClose}
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Change Impact Analysis</Text>
          <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
            <Text style={styles.closeButtonText}>✕</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.inputSection}>
          <Text style={styles.label}>Service Name:</Text>
          <TextInput
            style={styles.input}
            value={serviceName}
            onChangeText={setServiceName}
            placeholder="Enter service name (e.g., user-management-service)"
            autoCapitalize="none"
            autoCorrect={false}
          />
          <TouchableOpacity 
            onPress={analyzeImpact} 
            style={[styles.analyzeButton, loading && styles.disabledButton]}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.analyzeButtonText}>Analyze Impact</Text>
            )}
          </TouchableOpacity>
        </View>

        {error && (
          <View style={styles.errorContainer}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        {impactData && (
          <ScrollView style={styles.resultsContainer}>
            <View style={styles.summaryCard}>
              <Text style={styles.summaryTitle}>Impact Summary</Text>
              <Text style={styles.summaryText}>
                Target Service: <Text style={styles.highlight}>{impactData.targetService}</Text>
              </Text>
              <Text style={styles.summaryText}>
                Total Affected Services: <Text style={styles.highlight}>{impactData.totalAffected}</Text>
              </Text>
            </View>

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>
                Directly Affected Services ({impactData.directlyAffected.length})
              </Text>
              <Text style={styles.sectionDescription}>
                Services that immediately depend on this service
              </Text>
              {impactData.directlyAffected.map((service) => (
                <View key={service} style={styles.serviceItem}>
                  <View style={[styles.impactIndicator, styles.directImpact]} />
                  <Text style={styles.serviceName}>{service}</Text>
                </View>
              ))}
              {impactData.directlyAffected.length === 0 && (
                <Text style={styles.noDataText}>No directly affected services</Text>
              )}
            </View>

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>
                Indirectly Affected Services ({impactData.indirectlyAffected.length})
              </Text>
              <Text style={styles.sectionDescription}>
                Services that could be affected through cascade failures
              </Text>
              {impactData.indirectlyAffected.map((service) => (
                <View key={service} style={styles.serviceItem}>
                  <View style={[styles.impactIndicator, styles.indirectImpact]} />
                  <Text style={styles.serviceName}>{service}</Text>
                </View>
              ))}
              {impactData.indirectlyAffected.length === 0 && (
                <Text style={styles.noDataText}>No indirectly affected services</Text>
              )}
            </View>
          </ScrollView>
        )}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa'
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 20,
    paddingTop: 60,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef'
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
    color: '#2c3e50'
  },
  closeButton: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: '#ecf0f1',
    alignItems: 'center',
    justifyContent: 'center'
  },
  closeButtonText: {
    fontSize: 16,
    color: '#7f8c8d'
  },
  inputSection: {
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef'
  },
  label: {
    fontSize: 16,
    fontWeight: '500',
    color: '#34495e',
    marginBottom: 8
  },
  input: {
    borderWidth: 1,
    borderColor: '#bdc3c7',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    marginBottom: 16,
    backgroundColor: '#fff'
  },
  analyzeButton: {
    backgroundColor: '#3498db',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center'
  },
  disabledButton: {
    backgroundColor: '#95a5a6'
  },
  analyzeButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600'
  },
  errorContainer: {
    margin: 20,
    padding: 12,
    backgroundColor: '#f8d7da',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#f5c6cb'
  },
  errorText: {
    color: '#721c24',
    fontSize: 14
  },
  resultsContainer: {
    flex: 1,
    padding: 20
  },
  summaryCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 20,
    borderLeftWidth: 4,
    borderLeftColor: '#3498db'
  },
  summaryTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 8
  },
  summaryText: {
    fontSize: 14,
    color: '#5d6d7e',
    marginBottom: 4
  },
  highlight: {
    fontWeight: '600',
    color: '#2c3e50'
  },
  section: {
    marginBottom: 24
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#2c3e50',
    marginBottom: 4
  },
  sectionDescription: {
    fontSize: 13,
    color: '#7f8c8d',
    marginBottom: 12,
    fontStyle: 'italic'
  },
  serviceItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#fff',
    borderRadius: 8,
    marginBottom: 6
  },
  impactIndicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 12
  },
  directImpact: {
    backgroundColor: '#e74c3c'
  },
  indirectImpact: {
    backgroundColor: '#f39c12'
  },
  serviceName: {
    fontSize: 14,
    color: '#2c3e50',
    flex: 1
  },
  noDataText: {
    fontSize: 14,
    color: '#7f8c8d',
    fontStyle: 'italic',
    textAlign: 'center',
    paddingVertical: 20
  }
});

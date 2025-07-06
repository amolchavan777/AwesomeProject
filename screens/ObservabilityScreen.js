import React, { Component } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';

/**
 * Screen for ingesting observability data from monitoring tools
 * Supports Prometheus metrics, Jaeger traces, and OpenTelemetry spans
 */
class ObservabilityScreen extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: '',
      sourceId: '',
      isLoading: false,
      lastResult: null,
      sampleDataVisible: false,
    };
  }

  // Sample data for different observability formats
  getSampleData = () => {
    return `# Prometheus metrics
http_requests_total{service="api-gateway",target_service="auth-service"} 1250 1720180800
grpc_client_calls_total{service="auth-service",target_service="user-db"} 890 1720180800
database_connections_active{service="order-service",target_service="postgres-primary"} 25 1720180800

# Jaeger traces
2025-07-05T10:30:45.123Z trace_abc123 "payment-service" -> "stripe-api" 240ms
2025-07-05T10:30:46.456Z trace_def456 "user-service" -> "redis-cache" 15ms

# OpenTelemetry spans
span_id:span123 trace_id:trace456 service:notification-service operation:"send_email" downstream:smtp-relay duration:340ms status:OK
span_id:span789 trace_id:trace101 service:analytics-service operation:"process_events" downstream:kafka-cluster duration:125ms status:OK`;
  };

  // Load sample data into the input field
  loadSampleData = () => {
    this.setState({
      data: this.getSampleData(),
      sourceId: 'sample-observability-data',
      sampleDataVisible: false,
    });
  };

  // Clear all input fields
  clearData = () => {
    this.setState({
      data: '',
      sourceId: '',
      lastResult: null,
    });
  };

  // Submit observability data to the backend
  submitData = async () => {
    const { data, sourceId } = this.state;

    if (!data.trim()) {
      Alert.alert('Error', 'Please enter some observability data');
      return;
    }

    this.setState({ isLoading: true });

    try {
      const finalSourceId = sourceId.trim() || `observability-${Date.now()}`;
      const response = await fetch(`http://localhost:8082/api/ingest/observability?sourceId=${encodeURIComponent(finalSourceId)}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: data,
      });

      const result = await response.json();

      if (response.ok && result.success) {
        const { result: ingestionResult } = result;
        this.setState({ lastResult: ingestionResult });
        Alert.alert(
          'Success',
          `Observability data ingested successfully!\n\n` +
          `Source: ${ingestionResult.sourceId}\n` +
          `Claims extracted: ${ingestionResult.rawClaimsExtracted}\n` +
          `Claims saved: ${ingestionResult.claimsSaved}\n` +
          `Processing time: ${ingestionResult.processingTimeMs}ms`
        );
      } else {
        const errorMsg = result.error?.message || result.message || 'Unknown error occurred';
        Alert.alert('Error', `Failed to ingest data:\n${errorMsg}`);
      }
    } catch (error) {
      console.error('Error submitting observability data:', error);
      Alert.alert(
        'Error',
        'Failed to connect to the backend. Please ensure the server is running on localhost:8082'
      );
    } finally {
      this.setState({ isLoading: false });
    }
  };

  render() {
    const { data, sourceId, isLoading, lastResult, sampleDataVisible } = this.state;

    return (
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView style={styles.scrollContainer} showsVerticalScrollIndicator={false}>
          <View style={styles.header}>
            <Text style={styles.title}>Observability Data Ingestion</Text>
            <Text style={styles.subtitle}>
              Import dependency data from Prometheus, Jaeger, and OpenTelemetry
            </Text>
          </View>

          <View style={styles.inputSection}>
            <Text style={styles.label}>Source ID (optional)</Text>
            <TextInput
              style={styles.sourceInput}
              placeholder="e.g., prometheus-cluster-1"
              value={sourceId}
              onChangeText={(text) => this.setState({ sourceId: text })}
            />
          </View>

          <View style={styles.inputSection}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>Observability Data</Text>
              <TouchableOpacity
                style={styles.sampleButton}
                onPress={() => this.setState({ sampleDataVisible: !sampleDataVisible })}
              >
                <Text style={styles.sampleButtonText}>
                  {sampleDataVisible ? 'Hide' : 'Show'} Sample
                </Text>
              </TouchableOpacity>
            </View>
            
            {sampleDataVisible && (
              <View style={styles.sampleSection}>
                <Text style={styles.sampleTitle}>Supported Formats:</Text>
                <Text style={styles.sampleText}>
                  • Prometheus: http_requests_total{'{'}service="app",target_service="api"{'}'} 1250{'\n'}
                  • Jaeger: 2025-07-05T10:30:45.123Z trace_id "from" {'->'} "to" 240ms{'\n'}
                  • OpenTelemetry: span_id:abc trace_id:def service:app operation:"op" downstream:target duration:100ms status:OK
                </Text>
                <TouchableOpacity style={styles.loadSampleButton} onPress={this.loadSampleData}>
                  <Text style={styles.loadSampleButtonText}>Load Sample Data</Text>
                </TouchableOpacity>
              </View>
            )}

            <TextInput
              style={styles.dataInput}
              placeholder="Paste your observability data here..."
              value={data}
              onChangeText={(text) => this.setState({ data: text })}
              multiline
              textAlignVertical="top"
            />
          </View>

          <View style={styles.buttonSection}>
            <TouchableOpacity
              style={[styles.submitButton, isLoading && styles.disabledButton]}
              onPress={this.submitData}
              disabled={isLoading}
            >
              <Text style={styles.submitButtonText}>
                {isLoading ? 'Processing...' : 'Ingest Data'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.clearButton} onPress={this.clearData}>
              <Text style={styles.clearButtonText}>Clear</Text>
            </TouchableOpacity>
          </View>

          {lastResult && (
            <View style={styles.resultSection}>
              <Text style={styles.resultTitle}>Last Ingestion Result</Text>
              <View style={styles.resultItem}>
                <Text style={styles.resultLabel}>Source:</Text>
                <Text style={styles.resultValue}>{lastResult.sourceId}</Text>
              </View>
              <View style={styles.resultItem}>
                <Text style={styles.resultLabel}>Claims Extracted:</Text>
                <Text style={styles.resultValue}>{lastResult.rawClaimsExtracted}</Text>
              </View>
              <View style={styles.resultItem}>
                <Text style={styles.resultLabel}>Claims Saved:</Text>
                <Text style={styles.resultValue}>{lastResult.claimsSaved}</Text>
              </View>
              <View style={styles.resultItem}>
                <Text style={styles.resultLabel}>Processing Time:</Text>
                <Text style={styles.resultValue}>{lastResult.processingTimeMs}ms</Text>
              </View>
            </View>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollContainer: {
    flex: 1,
  },
  header: {
    backgroundColor: '#4a90e2',
    padding: 20,
    paddingTop: 40,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 16,
    color: '#e1f0ff',
    textAlign: 'center',
    marginTop: 8,
  },
  inputSection: {
    margin: 16,
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  labelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  sourceInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 6,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fafafa',
  },
  dataInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 6,
    padding: 12,
    fontSize: 14,
    height: 200,
    backgroundColor: '#fafafa',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
  },
  sampleButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#4a90e2',
  },
  sampleButtonText: {
    color: '#4a90e2',
    fontSize: 14,
    fontWeight: '500',
  },
  sampleSection: {
    backgroundColor: '#f8f9fa',
    padding: 12,
    borderRadius: 6,
    marginBottom: 12,
  },
  sampleTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  sampleText: {
    fontSize: 12,
    color: '#666',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    lineHeight: 18,
  },
  loadSampleButton: {
    backgroundColor: '#4a90e2',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 4,
    marginTop: 8,
    alignSelf: 'flex-start',
  },
  loadSampleButtonText: {
    color: 'white',
    fontSize: 12,
    fontWeight: '500',
  },
  buttonSection: {
    flexDirection: 'row',
    margin: 16,
    gap: 12,
  },
  submitButton: {
    flex: 1,
    backgroundColor: '#28a745',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  disabledButton: {
    backgroundColor: '#ccc',
  },
  submitButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  clearButton: {
    flex: 1,
    backgroundColor: '#6c757d',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  clearButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  resultSection: {
    margin: 16,
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  resultTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  resultItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  resultLabel: {
    fontSize: 14,
    color: '#666',
  },
  resultValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
});

export default ObservabilityScreen;

import React from 'react';
import { View, Text, FlatList, ActivityIndicator, StyleSheet } from 'react-native';

export default class DependencyDashboard extends React.Component {
  static navigationOptions = {
    title: 'Dependencies',
  };

  state = {
    dependencies: [],
    loading: true,
    error: null,
  };

  componentDidMount() {
    this._load();
  }

  _load = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/dependencies');
      if (!res.ok) throw new Error('Request failed');
      const deps = await res.json();
      this.setState({ dependencies: deps, loading: false });
    } catch (err) {
      this.setState({ error: err.message, loading: false });
    }
  };

  _renderItem = ({ item }) => (
    <View style={styles.item}>
      <Text>{item}</Text>
    </View>
  );

  render() {
    const { dependencies, loading, error } = this.state;
    if (loading) {
      return (
        <View style={styles.center}>
          <ActivityIndicator />
        </View>
      );
    }

    if (error) {
      return (
        <View style={styles.center}>
          <Text>Error: {error}</Text>
        </View>
      );
    }

    return (
      <FlatList
        data={dependencies}
        keyExtractor={(item, idx) => String(idx)}
        renderItem={this._renderItem}
        contentContainerStyle={dependencies.length ? null : styles.center}
        ListEmptyComponent={<Text>No dependencies</Text>}
      />
    );
  }
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  item: {
    padding: 15,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#ccc',
  },
});

import React from 'react';
import { Platform } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createStackNavigator } from '@react-navigation/stack';
import { Ionicons } from '@expo/vector-icons';

import HomeScreen from '../screens/HomeScreen';
import LinksScreen from '../screens/LinksScreen';
import SettingsScreen from '../screens/SettingsScreen';
import DependencyDashboard from '../screens/DependencyDashboard';
import AnalyticsScreen from '../screens/AnalyticsScreen';
import ServiceMeshScreen from '../screens/ServiceMeshScreen';
import DashboardScreen from '../screens/DashboardScreen';
import ObservabilityScreen from '../screens/ObservabilityScreen';

const Stack = createStackNavigator();
const Tab = createBottomTabNavigator();

function HomeStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Home" component={HomeScreen} />
    </Stack.Navigator>
  );
}

function LinksStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Links" component={LinksScreen} />
    </Stack.Navigator>
  );
}

function DependencyStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Dependencies" component={DependencyDashboard} />
    </Stack.Navigator>
  );
}

function AnalyticsStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Analytics" component={AnalyticsScreen} />
    </Stack.Navigator>
  );
}

function SettingsStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Settings" component={SettingsScreen} />
    </Stack.Navigator>
  );
}

function ServiceMeshStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="ServiceMesh" component={ServiceMeshScreen} />
    </Stack.Navigator>
  );
}

function DashboardStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Dashboard" component={DashboardScreen} />
    </Stack.Navigator>
  );
}

function ObservabilityStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Observability" component={ObservabilityScreen} />
    </Stack.Navigator>
  );
}

export default function MainTabNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName;

          if (route.name === 'Dashboard') {
            iconName = Platform.OS === 'ios' 
              ? (focused ? 'ios-speedometer' : 'ios-speedometer-outline')
              : 'speedometer';
          } else if (route.name === 'Home') {
            iconName = Platform.OS === 'ios' 
              ? (focused ? 'ios-information-circle' : 'ios-information-circle-outline')
              : 'information-circle';
          } else if (route.name === 'Deps') {
            iconName = Platform.OS === 'ios' 
              ? (focused ? 'ios-analytics' : 'ios-analytics-outline')
              : 'analytics';
          } else if (route.name === 'Analytics') {
            iconName = Platform.OS === 'ios' 
              ? (focused ? 'ios-stats-chart' : 'ios-stats-chart-outline')
              : 'stats-chart';
          } else if (route.name === 'Mesh') {
            iconName = Platform.OS === 'ios' 
              ? (focused ? 'ios-git-network' : 'ios-git-network-outline')
              : 'git-network';
          } else if (route.name === 'Monitoring') {
            iconName = Platform.OS === 'ios' 
              ? (focused ? 'ios-cloud' : 'ios-cloud-outline')
              : 'cloud';
          }

          return <Ionicons name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: 'tomato',
        tabBarInactiveTintColor: 'gray',
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardStack} />
      <Tab.Screen name="Home" component={HomeStack} />
      <Tab.Screen name="Deps" component={DependencyStack} />
      <Tab.Screen name="Analytics" component={AnalyticsStack} />
      <Tab.Screen name="Mesh" component={ServiceMeshStack} />
      <Tab.Screen name="Monitoring" component={ObservabilityStack} />
    </Tab.Navigator>
  );
}

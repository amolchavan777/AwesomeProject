import React from 'react';
import { Platform } from 'react-native';
import { createStackNavigator, createBottomTabNavigator } from 'react-navigation';

import TabBarIcon from '../components/TabBarIcon';
import HomeScreen from '../screens/HomeScreen';
import LinksScreen from '../screens/LinksScreen';
import SettingsScreen from '../screens/SettingsScreen';
import DependencyDashboard from '../screens/DependencyDashboard';
import AnalyticsScreen from '../screens/AnalyticsScreen';

const HomeStack = createStackNavigator({
  Home: HomeScreen,
});

function getHomeIconName(focused) {
  if (Platform.OS === 'ios') {
    return focused ? 'ios-information-circle' : 'ios-information-circle-outline';
  } else {
    return 'md-information-circle';
  }
}

HomeStack.navigationOptions = {
  tabBarLabel: 'Home',
  tabBarIcon: ({ focused }) => (
    <TabBarIcon
      focused={focused}
      name={getHomeIconName(focused)}
    />
  ),
};

const LinksStack = createStackNavigator({
  Links: LinksScreen,
});

LinksStack.navigationOptions = {
  tabBarLabel: 'Links',
  tabBarIcon: ({ focused }) => (
    <TabBarIcon
      focused={focused}
      name={Platform.OS === 'ios' ? 'ios-link' : 'md-link'}
    />
  ),
}; 

const DependencyStack = createStackNavigator({
  Dependencies: DependencyDashboard,
});

DependencyStack.navigationOptions = {
  tabBarLabel: 'Deps',
  tabBarIcon: ({ focused }) => (
    <TabBarIcon
      focused={focused}
      name={Platform.OS === 'ios' ? 'ios-analytics' : 'md-analytics'}
    />
  ),
};

const AnalyticsStack = createStackNavigator({
  Analytics: AnalyticsScreen,
});

AnalyticsStack.navigationOptions = {
  tabBarLabel: 'Analytics',
  tabBarIcon: ({ focused }) => (
    <TabBarIcon
      focused={focused}
      name={Platform.OS === 'ios' ? 'ios-stats' : 'md-stats'}
    />
  ),
};

const SettingsStack = createStackNavigator({
  Settings: SettingsScreen,
});

SettingsStack.navigationOptions = {
  tabBarLabel: 'Settings',
  tabBarIcon: ({ focused }) => (
    <TabBarIcon
      focused={focused}
      name={Platform.OS === 'ios' ? 'ios-options' : 'md-options'}
    />
  ),
};

export default createBottomTabNavigator({
  HomeStack,
  LinksStack,
  DependencyStack,
  AnalyticsStack,
  SettingsStack,
});

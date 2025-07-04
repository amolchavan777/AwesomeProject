const { registerAdapter, createAdapter } = require('./AdapterFactory');
const RouterLogAdapter = require('./RouterLogAdapter');
const CodebaseAdapter = require('./CodebaseAdapter');
const CICDAdapter = require('./CICDAdapter');
const ApiGatewayAdapter = require('./ApiGatewayAdapter');
const TelemetryAdapter = require('./TelemetryAdapter');
const NetworkLogAdapter = require('./NetworkLogAdapter');

// register built-in adapters
registerAdapter('routerLog', RouterLogAdapter);
registerAdapter('codebase', CodebaseAdapter);
registerAdapter('cicd', CICDAdapter);
registerAdapter('apiGateway', ApiGatewayAdapter);
registerAdapter('telemetry', TelemetryAdapter);
registerAdapter('networkLog', NetworkLogAdapter);

module.exports = {
  registerAdapter,
  createAdapter,
  RouterLogAdapter,
  CodebaseAdapter,
  CICDAdapter,
  ApiGatewayAdapter,
  TelemetryAdapter,
  NetworkLogAdapter,
};

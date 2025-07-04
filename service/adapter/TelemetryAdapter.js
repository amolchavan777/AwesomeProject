const DataSourceAdapter = require('./DataSourceAdapter');

class TelemetryAdapter extends DataSourceAdapter {
  constructor(options = {}) {
    super();
    this.metrics = options.metrics || [];
  }

  fetch() {
    // Return provided metrics; in real world this would query telemetry system
    return this.metrics;
  }
}

module.exports = TelemetryAdapter;

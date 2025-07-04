const DataSourceAdapter = require('./DataSourceAdapter');
const fs = require('fs');

class NetworkLogAdapter extends DataSourceAdapter {
  constructor(options = {}) {
    super();
    this.logFile = options.logFile || '';
  }

  fetch() {
    if (!this.logFile || !fs.existsSync(this.logFile)) {
      return null;
    }
    const raw = fs.readFileSync(this.logFile, 'utf8');
    return this.parse(raw);
  }
}

module.exports = NetworkLogAdapter;

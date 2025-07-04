const DataSourceAdapter = require('./DataSourceAdapter');
const RouterLogBuilder = require('./builders/RouterLogBuilder');
const fs = require('fs');

class RouterLogAdapter extends DataSourceAdapter {
  constructor(options = {}) {
    super(new RouterLogBuilder());
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

module.exports = RouterLogAdapter;

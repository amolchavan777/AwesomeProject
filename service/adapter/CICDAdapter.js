const DataSourceAdapter = require('./DataSourceAdapter');

class CICDAdapter extends DataSourceAdapter {
  constructor(options = {}) {
    super();
    this.env = options.env || process.env;
  }

  fetch() {
    // Return minimal CI/CD related environment variables
    const keys = ['CI', 'GITHUB_ACTIONS', 'TRAVIS'];
    const data = {};
    keys.forEach(k => {
      if (this.env[k]) data[k] = this.env[k];
    });
    return data;
  }
}

module.exports = CICDAdapter;

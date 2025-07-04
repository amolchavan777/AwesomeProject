const DataSourceAdapter = require('./DataSourceAdapter');
const fetch = require('node-fetch');

class ApiGatewayAdapter extends DataSourceAdapter {
  constructor(options = {}) {
    super();
    this.endpoint = options.endpoint || '';
  }

  async fetch() {
    if (!this.endpoint) return null;
    const res = await fetch(this.endpoint).catch(() => null);
    if (!res || !res.ok) return null;
    const data = await res.json();
    return data;
  }
}

module.exports = ApiGatewayAdapter;

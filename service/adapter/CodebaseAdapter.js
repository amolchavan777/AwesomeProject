const DataSourceAdapter = require('./DataSourceAdapter');
const fs = require('fs');

class CodebaseAdapter extends DataSourceAdapter {
  constructor(options = {}) {
    super();
    this.path = options.path || process.cwd();
  }

  fetch() {
    if (!fs.existsSync(this.path)) return null;
    const files = fs.readdirSync(this.path);
    return files;
  }
}

module.exports = CodebaseAdapter;

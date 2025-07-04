class RouterLogBuilder {
  constructor() {
    this.reset();
  }

  reset() {
    this.log = {};
    this.raw = '';
  }

  withRaw(raw) {
    this.raw = raw;
    return this;
  }

  // Example parse implementation
  parse(raw) {
    this.withRaw(raw);
    const parts = String(this.raw).trim().split('|');
    this.log.timestamp = parts[0] || null;
    this.log.level = parts[1] || null;
    this.log.message = parts.slice(2).join('|') || null;
    return this;
  }

  build() {
    const result = this.log;
    this.reset();
    return result;
  }
}

module.exports = RouterLogBuilder;

class DataSourceAdapter {
  constructor(builder) {
    this.builder = builder;
  }

  // Fetch raw data from the source. Subclasses should override.
  fetch(_input) {
    throw new Error('fetch() must be implemented by subclass');
  }

  // Parse raw data using a builder when provided
  parse(raw) {
    if (this.builder && typeof this.builder.parse === 'function') {
      return this.builder.parse(raw).build();
    }
    return raw;
  }
}

module.exports = DataSourceAdapter;

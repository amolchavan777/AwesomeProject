const adapters = {};

function registerAdapter(name, adapterClass) {
  adapters[name] = adapterClass;
}

function createAdapter(name, options) {
  const Adapter = adapters[name];
  if (!Adapter) throw new Error(`Adapter ${name} not registered`);
  return new Adapter(options);
}

module.exports = { registerAdapter, createAdapter };

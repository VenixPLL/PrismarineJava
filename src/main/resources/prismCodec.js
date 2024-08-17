const { createDeserializer, createSerializer } = require('minecraft-protocol');

const serializerMap = new Map();
const deserializerMap = new Map();

function getOrCreateSerializer(state, version, isServer) {
  const key = `${state}_${version}_${isServer}`;
  if (!serializerMap.has(key)) {
    serializerMap.set(key, createSerializer({ state, version, isServer }));
  }
  return serializerMap.get(key);
}

function getOrCreateDeserializer(state, version, isServer) {
  const key = `${state}_${version}_${isServer}`;
  if (!deserializerMap.has(key)) {
    deserializerMap.set(key, createDeserializer({ state, version, isServer }));
  }
  return deserializerMap.get(key);
}

function convertObjectToBuffer(state, version, json, isServer) {
  const serializer = getOrCreateSerializer(state, version, isServer);

  const data = serializer.createPacketBuffer(JSON.parse(json));
  const arrByte = Uint8Array.from(data);

  return arrByte;
}

function convertBufferToObject(state, version, buffer, isServer) {
  const buf = Buffer.from(buffer);
  const deserializer = getOrCreateDeserializer(state, version, isServer);
  const data = deserializer.parsePacketBuffer(buf).data;
  const out = JSON.stringify(data);

  return out;
}


function keepAlive() {
  return "keepAlive";
}
# Prismarine Java (PrismJava)
### [Node.js](https://nodejs.org/en) Interop using [Javet](https://github.com/caoccao/Javet), allows you to access [PrismarineJS node-minecraft-protocol](https://github.com/PrismarineJS/node-minecraft-protocol) using Java

# WARNING
### This project is not stable! Concurrency issues will occur when using 2 or more threads for connections!  
### Keep in mind that this project is using 3rd party packet decoder/encoder! any issues with encoding decoding packets that occur inside node-minecraft-protocol should be reported to the correct repository!

## Example usage
### Check out test folder in source.

# Known Bugs
- Concurrency causes Javet to lock up when 2 or more threads try to decode/encode at the same time  
  (Suggested fix: add a concurrent queue for decode/encode, or make multiple different instances of JavetInterop for different threads)
- Versions above 1.20.1 will not decode most of the packets including ex: **bundle_delimiter**,**declare_commands**  
  (Suggested fix: Unknown)

# TODO
- Add encryption support.
- Resolve [issue with custom_payloads](https://github.com/PrismarineJS/node-minecraft-protocol/issues/1256)  
  (Was resolved using legacy packets, but to make this library simpler fix it the proper way)
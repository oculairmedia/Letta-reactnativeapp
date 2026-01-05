# Letta AI - Mobile Successor to Letta

<div align="center">
  <img src="assets/images/app-icon-all.png" alt="Letta AI Logo" width="200" />
</div>

> A powerful mobile framework for creating LLM services with infinite memory, built on the foundations of [Letta](https://github.com/ahmedrowaihi/letta)

[![Discord](https://img.shields.io/discord/1161736243340640419?label=Discord&logo=discord&logoColor=5865F2&style=flat-square&color=5865F2)](https://discord.gg/letta)
[![Twitter Follow](https://img.shields.io/badge/Follow-%40Letta__AI-1DA1F2?style=flat-square&logo=x&logoColor=white)](https://twitter.com/Letta_AI)
[![arxiv 2310.08560](https://img.shields.io/badge/Research-2310.08560-B31B1B?logo=arxiv&style=flat-square)](https://arxiv.org/abs/2310.08560)
[![GitHub](https://img.shields.io/github/stars/ahmedrowaihi/react-native-letta?style=flat-square&logo=github&label=Stars&color=gold)](https://github.com/ahmedrowaihi/react-native-letta)

This mobile application is built using [Infinite Red's](https://infinite.red) Ignite boilerplate, bringing the power of Letta's memory management to mobile devices. It combines the robust memory handling capabilities of Letta with the sleek and performant mobile experience provided by React Native.

## Features

- 📱 Native mobile experience for both iOS and Android
- 🧠 Infinite memory management for LLMs
- 💾 Seamless integration with Letta's backend services
- ⚡ High-performance React Native implementation
- 🎨 Beautiful and intuitive mobile UI

## Getting Started

First, ensure you have the development environment set up:

```bash
yarn install
yarn prebuild:clean
yarn start
```

To build for your target platform, you'll need to [run `eas build`](https://github.com/infinitered/ignite/blob/master/docs/expo/EAS.md). We provide several convenient shortcuts:

```bash
yarn build:ios:sim # build for ios simulator
yarn build:ios:dev # build for ios device
yarn build:ios:prod # build for ios device
```

### Backend Configuration

This mobile app requires a running instance of Letta server. You can either:

- Use the official Letta cloud service
- Run your own Letta server instance locally

For local development, make sure to configure your environment variables appropriately.

### `./assets` directory

The assets are organized into subdirectories for easy management:

```tree
assets
├── icons
└── images
```

**icons**
Store your application icons here. These are used throughout the UI for navigation and interactive elements. The recommended format is PNG.

**images**
Store your application images here, including logos, backgrounds, and other graphics.

Example usage of assets:

```typescript
import { Image } from 'react-native';

const MyComponent = () => {
  return (
    <Image source={require('../assets/images/my_image.png')} />
  );
};
```

## Testing

For end-to-end testing, we use Maestro. Follow our [Maestro Setup](https://ignitecookbook.com/docs/recipes/MaestroSetup) guide to get started.

## Documentation

- [Ignite Documentation](https://github.com/infinitered/ignite/blob/master/docs/README.md)
- [Letta Documentation](https://github.com/ahmedrowaihi/letta)

## Community

Join our vibrant community and get involved in the project:

- ⭐️ Support the project:
  - [Star React Native Letta](https://github.com/ahmedrowaihi/react-native-letta)
  - [Star Letta Core](https://github.com/ahmedrowaihi/letta)
- 💬 Join the discussion:
  - [Letta Discord](https://discord.gg/letta) - Join our #mobile channel
- 📰 Stay updated:
  - [Follow @Letta_AI on Twitter](https://twitter.com/Letta_AI)
  - [Read our Research Paper](https://arxiv.org/abs/2310.08560)
- 🤝 Contribute:
  - [Report Mobile Issues](https://github.com/ahmedrowaihi/react-native-letta/issues)
  - [Join community events](https://lu.ma/berkeley-llm-meetup)
  - [View the roadmap](https://github.com/ahmedrowaihi/react-native-letta/issues)

## Legal

By using Letta AI and related services, you agree to the privacy policy and terms of service of both Letta and Infinite Red.

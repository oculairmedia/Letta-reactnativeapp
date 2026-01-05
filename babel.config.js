/** @type {import('@babel/core').TransformOptions} */
module.exports = function (api) {
  api.cache(true)
  return {
    presets: ["babel-preset-expo"],
    plugins: [
      [
        "module-resolver",
        {
          alias: {
            "better-auth/react": "./node_modules/better-auth/dist/client/react/index.cjs",
            "better-auth/client/plugins":
              "./node_modules/better-auth/dist/client/plugins/index.cjs",
            "@better-auth/expo/client": "./node_modules/@better-auth/expo/dist/client.cjs",
          },
        },
      ],
    ],
  }
}

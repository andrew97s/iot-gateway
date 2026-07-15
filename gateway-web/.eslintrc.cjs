/**
 * @type {import('eslint').Linter.Config}
 */
module.exports = {
  root: true,
  env: {
    node: true,
    browser: true
  },
  extends: ['eslint:recommended', './.eslintrc-auto-import.json', 'plugin:prettier/recommended', 'plugin:vue/vue3-recommended'],
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest'
  },
  globals: {
    hostConfig: true,
    Jessibuca: true
  },
  ignorePatterns: ['public/**', 'src/assets/**', 'types/**'],
  rules: {
    'no-unused-vars': 'off',
    'no-unused-labels': 'off',
    'no-empty': 'off',
    'no-extra-semi': 'off',
    'prettier/prettier': ['error'],
    'vue/multi-word-component-names': 'off',
    'vue/no-side-effects-in-computed-properties': 'off',
    'vue/no-mutating-props': [
      'error',
      {
        shallowOnly: true
      }
    ],
    'vue/singleline-html-element-content-newline': 'off',
    'vue/html-self-closing': 'off',
    'vue/max-attributes-per-line': [
      'error',
      {
        singleline: {
          max: Infinity
        },
        multiline: {
          max: 1
        }
      }
    ],
    'vue/require-default-prop': 'error',
    'vue/v-on-event-hyphenation': 'off',
    'vue/attribute-hyphenation': 'error',
    'vue/attributes-order': [
      'error',
      {
        order: [
          'GLOBAL',
          'TWO_WAY_BINDING',
          'LIST_RENDERING',
          'OTHER_ATTR',
          'DEFINITION',
          'RENDER_MODIFIERS',
          ['UNIQUE', 'SLOT'],
          'CONTENT',
          'OTHER_DIRECTIVES',
          'EVENTS',
          'CONDITIONALS'
        ],
        alphabetical: true
      }
    ],
    'vue/no-dupe-keys': 'off',
    'vue/no-template-shadow': 'error',
    'vue/no-unused-vars': 'off',
    'vue/no-v-html': 'off'
  }
}

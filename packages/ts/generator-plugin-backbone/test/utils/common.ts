import { readFile } from 'node:fs/promises';
import Generator from '@haru/generator-core/Generator.js';
import type { PluginConstructor } from '@haru/generator-core/Plugin.js';
import LoggerFactory from '@haru/generator-utils/LoggerFactory.js';

export const pathBase = 'com/vaadin/hilla/parser/plugins/backbone';

export function createGenerator(plugins: readonly PluginConstructor[], outputDir?: string): Generator {
  return new Generator(plugins, { logger: new LoggerFactory({ name: 'tsgen-test', verbose: true }), outputDir });
}

export async function loadInput(name: string, importMeta: string): Promise<string> {
  return readFile(new URL(`./${name}.json`, importMeta), 'utf8');
}

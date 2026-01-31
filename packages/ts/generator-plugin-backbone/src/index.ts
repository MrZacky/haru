import Plugin from '@vaadin/hilla-generator-core/Plugin.js';
import type { SharedStorage } from '@vaadin/hilla-generator-core/SharedStorage.js';
import { OpenAPIV3 } from 'openapi-types';
import type { SourceFile } from 'typescript';
import EndpointProcessor from './EndpointProcessor.js';
import { EntityProcessor } from './EntityProcessor.js';

export enum BackbonePluginSourceType {
  Endpoint = 'endpoint',
  Entity = 'entity',
}

export interface OperationInfo {
  path: string;
  httpMethod: OpenAPIV3.HttpMethods;
  operation: OpenAPIV3.OperationObject;
  pathParameters: OpenAPIV3.ParameterObject[];
}

export default class BackbonePlugin extends Plugin {
  static readonly BACKBONE_PLUGIN_FILE_TAGS = 'BACKBONE_PLUGIN_FILE_TAGS';
  declare ['constructor']: typeof BackbonePlugin;
  readonly #tags = new WeakMap<SourceFile, BackbonePluginSourceType>();

  override get path(): string {
    return import.meta.url;
  }

  override async execute(storage: SharedStorage): Promise<void> {
    const endpointSourceFiles = await this.#processEndpoints(storage);
    const entitySourceFiles = this.#processEntities(storage);

    endpointSourceFiles.forEach((file) => this.#tags.set(file, BackbonePluginSourceType.Endpoint));
    entitySourceFiles.forEach((file) => this.#tags.set(file, BackbonePluginSourceType.Entity));

    storage.sources.push(...endpointSourceFiles, ...entitySourceFiles);
    storage.pluginStorage.set(this.constructor.BACKBONE_PLUGIN_FILE_TAGS, this.#tags);
  }

  async #processEndpoints(storage: SharedStorage): Promise<readonly SourceFile[]> {
    this.logger.debug('Processing endpoints');
    const tagGroups = new Map<string, OperationInfo[]>();

    Object.entries(storage.api.paths)
      .filter(([, pathItem]) => !!pathItem)
      .forEach(([path, pathItem]) => {
        // Collect path-level parameters
        const pathLevelParams = ((pathItem as OpenAPIV3.PathItemObject).parameters ?? []).map((p) =>
          this.resolver.resolve(p),
        );

        for (const httpMethod of Object.values(OpenAPIV3.HttpMethods)) {
          const operation = (pathItem as OpenAPIV3.PathItemObject)[httpMethod];
          if (!operation) continue;

          // Merge path-level and operation-level parameters
          const operationParams = (operation.parameters ?? []).map((p) => this.resolver.resolve(p));
          const allPathParams = [...pathLevelParams, ...operationParams].filter((p) => p.in === 'path');

          // Group by first tag, or derive from first path segment
          const tag = operation.tags?.[0] ?? path.split('/').find(Boolean) ?? 'Default';

          if (!tagGroups.has(tag)) {
            tagGroups.set(tag, []);
          }
          tagGroups.get(tag)!.push({ path, httpMethod, operation, pathParameters: allPathParams });
        }
      });

    const processors = await Promise.all(
      Array.from(tagGroups.entries(), async ([tagName, operations]) =>
        EndpointProcessor.create(tagName, operations, storage, this),
      ),
    );

    return Promise.all(processors.map(async (processor) => processor.process()));
  }

  #processEntities(storage: SharedStorage): readonly SourceFile[] {
    this.logger.debug('Processing entities');

    return storage.api.components?.schemas
      ? Object.entries(storage.api.components.schemas).map(([name, component]) =>
          new EntityProcessor(name, component, storage, this).process(),
        )
      : [];
  }
}

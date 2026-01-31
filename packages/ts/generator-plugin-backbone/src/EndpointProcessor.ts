import type Plugin from '@vaadin/hilla-generator-core/Plugin.js';
import type { SharedStorage, TransferTypes } from '@vaadin/hilla-generator-core/SharedStorage.js';
import ClientPlugin from '@vaadin/hilla-generator-plugin-client';
import createSourceFile from '@vaadin/hilla-generator-utils/createSourceFile.js';
import DependencyManager from '@vaadin/hilla-generator-utils/dependencies/DependencyManager.js';
import PathManager from '@vaadin/hilla-generator-utils/dependencies/PathManager.js';
import type { SourceFile, Statement } from 'typescript';
import EndpointMethodOperationProcessor from './EndpointMethodOperationProcessor.js';
import type { OperationInfo } from './index.js';

export default class EndpointProcessor {
  static async create(
    name: string,
    operations: OperationInfo[],
    storage: SharedStorage,
    owner: Plugin,
  ): Promise<EndpointProcessor> {
    const endpoint = new EndpointProcessor(name, operations, storage, owner);
    endpoint.#dependencies.imports.default.add(
      endpoint.#dependencies.paths.createRelativePath(await ClientPlugin.getClientFileName(storage.outputDir)),
      'client',
    );
    return endpoint;
  }

  readonly #createdFilePaths = new PathManager({ extension: 'ts' });
  readonly #dependencies = new DependencyManager(new PathManager({ extension: '.js' }));
  readonly #operations: OperationInfo[];
  readonly #name: string;
  readonly #outputDir: string | undefined;
  readonly #transferTypes: TransferTypes;
  readonly #owner: Plugin;

  private constructor(name: string, operations: OperationInfo[], storage: SharedStorage, owner: Plugin) {
    this.#name = name;
    this.#owner = owner;
    this.#operations = operations;
    this.#outputDir = storage.outputDir;
    this.#transferTypes = storage.transferTypes;
  }

  async process(): Promise<SourceFile> {
    this.#owner.logger.debug(`Processing endpoint: ${this.#name}`);

    const statements = (await Promise.all(this.#operations.map(async (op) => this.#processOperation(op)))).filter(
      Boolean,
    ) as Statement[];

    const { exports, imports } = this.#dependencies;

    return createSourceFile(
      [...imports.toCode(), ...statements, ...exports.toCode()],
      this.#createdFilePaths.createRelativePath(this.#name),
    );
  }

  async #processOperation(op: OperationInfo): Promise<Statement | undefined> {
    const functionName = this.#deriveFunctionName(op);
    this.#owner.logger.debug(
      `Processing operation: ${this.#name}.${functionName} (${op.httpMethod.toUpperCase()} ${op.path})`,
    );

    const processor = EndpointMethodOperationProcessor.createProcessor(
      op.httpMethod,
      op.path,
      functionName,
      op.operation,
      op.pathParameters,
      this.#dependencies,
      this.#transferTypes,
      this.#owner,
    );

    return processor.process(this.#outputDir);
  }

  #deriveFunctionName(op: OperationInfo): string {
    const { operationId } = op.operation;
    if (!operationId) {
      // Derive from HTTP method + path: GET /pets/{petId} → getPetsPetId
      const pathPart = op.path
        .replace(/[{}]/gu, '')
        .split('/')
        .filter(Boolean)
        .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
        .join('');
      return op.httpMethod.toLowerCase() + pathPart;
    }

    // Strip Vaadin-style prefix/suffix: "TagName_methodName_METHOD"
    const vaadinPattern = new RegExp(`^${this.#name}_(.+)_${op.httpMethod.toUpperCase()}$`, 'u');
    const match = operationId.match(vaadinPattern);
    if (match) {
      return match[1];
    }

    // Use operationId as-is for standard REST APIs
    return operationId;
  }
}

/* eslint-disable max-params */
import type Plugin from '@vaadin/hilla-generator-core/Plugin.js';
import type { TransferTypes } from '@vaadin/hilla-generator-core/SharedStorage.js';
import ClientPlugin from '@vaadin/hilla-generator-plugin-client';
import type DependencyManager from '@vaadin/hilla-generator-utils/dependencies/DependencyManager.js';
import equal from 'fast-deep-equal';
import { OpenAPIV3 } from 'openapi-types';
import ts, { type Expression, type Statement, type TypeNode } from 'typescript';
import EndpointMethodRequestBodyProcessor from './EndpointMethodRequestBodyProcessor.js';
import EndpointMethodResponseProcessor from './EndpointMethodResponseProcessor.js';

export type EndpointMethodOperation = OpenAPIV3.OperationObject;

export default class EndpointMethodOperationProcessor {
  readonly #dependencies: DependencyManager;
  readonly #transferTypes: TransferTypes;
  readonly #functionName: string;
  readonly #httpMethod: OpenAPIV3.HttpMethods;
  readonly #path: string;
  readonly #operation: EndpointMethodOperation;
  readonly #pathParameters: OpenAPIV3.ParameterObject[];
  readonly #owner: Plugin;

  // eslint-disable-next-line @typescript-eslint/max-params
  static createProcessor(
    httpMethod: OpenAPIV3.HttpMethods,
    path: string,
    functionName: string,
    operation: EndpointMethodOperation,
    pathParameters: OpenAPIV3.ParameterObject[],
    dependencies: DependencyManager,
    transferTypes: TransferTypes,
    owner: Plugin,
  ): EndpointMethodOperationProcessor {
    return new EndpointMethodOperationProcessor(
      httpMethod,
      path,
      functionName,
      operation,
      pathParameters,
      dependencies,
      transferTypes,
      owner,
    );
  }

  // eslint-disable-next-line @typescript-eslint/max-params
  constructor(
    httpMethod: OpenAPIV3.HttpMethods,
    path: string,
    functionName: string,
    operation: EndpointMethodOperation,
    pathParameters: OpenAPIV3.ParameterObject[],
    dependencies: DependencyManager,
    transferTypes: TransferTypes,
    owner: Plugin,
  ) {
    this.#owner = owner;
    this.#dependencies = dependencies;
    this.#httpMethod = httpMethod;
    this.#path = path;
    this.#functionName = functionName;
    this.#operation = operation;
    this.#pathParameters = pathParameters;
    this.#transferTypes = transferTypes;
  }

  async process(outputDir?: string): Promise<Statement | undefined> {
    const { exports, imports, paths } = this.#dependencies;
    this.#owner.logger.debug(`${this.#functionName} - processing ${this.#httpMethod.toUpperCase()} ${this.#path}`);

    const hasRequestBody =
      this.#httpMethod === OpenAPIV3.HttpMethods.POST ||
      this.#httpMethod === OpenAPIV3.HttpMethods.PUT ||
      this.#httpMethod === OpenAPIV3.HttpMethods.PATCH;

    const { initParam, packedParameters, parameters } = new EndpointMethodRequestBodyProcessor(
      hasRequestBody ? this.#operation.requestBody : undefined,
      this.#dependencies,
      this.#transferTypes,
      this.#owner,
      this.#pathParameters,
      this.#operation.parameters as OpenAPIV3.ParameterObject[] | undefined,
    ).process();

    const methodIdentifier = exports.named.add(this.#functionName);
    const clientLibIdentifier = imports.default.getIdentifier(
      paths.createRelativePath(await ClientPlugin.getClientFileName(outputDir)),
    )!;

    const callExpression = ts.factory.createCallExpression(
      ts.factory.createPropertyAccessExpression(clientLibIdentifier, ts.factory.createIdentifier('call')),
      undefined,
      [
        ts.factory.createStringLiteral(this.#httpMethod.toUpperCase()),
        ts.factory.createStringLiteral(this.#path),
        packedParameters,
        initParam,
      ].filter(Boolean) as readonly Expression[],
    );

    const responseType = this.#prepareResponseType();

    return ts.factory.createFunctionDeclaration(
      [ts.factory.createToken(ts.SyntaxKind.AsyncKeyword)],
      undefined,
      methodIdentifier,
      undefined,
      parameters,
      ts.factory.createTypeReferenceNode('Promise', [responseType]),
      ts.factory.createBlock([ts.factory.createReturnStatement(callExpression)]),
    );
  }

  #prepareResponseType(): TypeNode {
    this.#owner.logger.debug(`${this.#functionName} ${this.#httpMethod.toUpperCase()} - processing response type`);

    const responseTypes = Object.entries(this.#operation.responses)
      .flatMap(([code, response]) =>
        new EndpointMethodResponseProcessor(
          code,
          response,
          this.#dependencies,
          this.#transferTypes,
          this.#owner,
        ).process(),
      )
      .filter((value, index, arr) => arr.findIndex((v) => equal(v, value)) === index);

    if (responseTypes.length === 0) {
      return ts.factory.createKeywordTypeNode(ts.SyntaxKind.VoidKeyword);
    }

    return ts.factory.createUnionTypeNode(responseTypes);
  }
}

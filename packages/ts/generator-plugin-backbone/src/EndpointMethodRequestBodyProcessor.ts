import type Plugin from '@vaadin/hilla-generator-core/Plugin.js';
import {
  isEmptyObject,
  isObjectSchema,
  type NonEmptyObjectSchema,
  type Schema,
} from '@vaadin/hilla-generator-core/Schema.js';
import type { TransferTypes } from '@vaadin/hilla-generator-core/SharedStorage.js';
import type DependencyManager from '@vaadin/hilla-generator-utils/dependencies/DependencyManager.js';
import type { OpenAPIV3 } from 'openapi-types';
import ts, { type Identifier, type ObjectLiteralExpression, type ParameterDeclaration } from 'typescript';
import TypeSchemaProcessor from './TypeSchemaProcessor.js';
import { defaultMediaType } from './utils.js';

export type EndpointMethodRequestBody = OpenAPIV3.RequestBodyObject;

export type EndpointMethodRequestBodyProcessingResult = Readonly<{
  parameters: readonly ParameterDeclaration[];
  packedParameters?: ObjectLiteralExpression;
  initParam: Identifier;
}>;

const DEFAULT_INIT_PARAM_NAME = 'init';
const INIT_TYPE_NAME = 'ClientRequestInit';

export default class EndpointMethodRequestBodyProcessor {
  readonly #dependencies: DependencyManager;
  readonly #transferTypes: TransferTypes;
  readonly #owner: Plugin;
  readonly #requestBody?: EndpointMethodRequestBody;
  readonly #pathParameters: OpenAPIV3.ParameterObject[];
  readonly #queryParameters: OpenAPIV3.ParameterObject[];
  readonly #clientModulePath: string;

  // eslint-disable-next-line @typescript-eslint/max-params
  constructor(
    requestBody: OpenAPIV3.ReferenceObject | OpenAPIV3.RequestBodyObject | undefined,
    dependencies: DependencyManager,
    transferTypes: TransferTypes,
    owner: Plugin,
    clientModulePath: string,
    pathParameters?: OpenAPIV3.ParameterObject[],
    operationParameters?: OpenAPIV3.ParameterObject[],
  ) {
    this.#owner = owner;
    this.#dependencies = dependencies;
    this.#requestBody = requestBody ? owner.resolver.resolve(requestBody) : undefined;
    this.#transferTypes = transferTypes;
    this.#clientModulePath = clientModulePath;
    this.#pathParameters = pathParameters ?? [];
    this.#queryParameters = (operationParameters ?? []).filter((p) => p.in === 'query');
  }

  process(): EndpointMethodRequestBodyProcessingResult {
    const { imports, paths } = this.#dependencies;
    const clientPath = paths.createRelativePath(this.#clientModulePath);
    const initTypeIdentifier =
      imports.named.getIdentifier(clientPath, INIT_TYPE_NAME) ?? imports.named.add(clientPath, INIT_TYPE_NAME);

    // Collect all parameter data: path params + query params + request body params
    const allParameterData: Array<readonly [string, Schema]> = [];

    // Add path parameters
    for (const param of this.#pathParameters) {
      if (param.schema) {
        const schema = this.#owner.resolver.resolve(param.schema) as Schema;
        allParameterData.push([param.name, schema]);
      }
    }

    // Add query parameters
    for (const param of this.#queryParameters) {
      if (param.schema) {
        const schema = this.#owner.resolver.resolve(param.schema) as Schema;
        allParameterData.push([param.name, schema]);
      }
    }

    // Add request body parameters
    if (this.#requestBody) {
      const bodyParams = this.#extractParameterData(this.#requestBody.content[defaultMediaType].schema);
      allParameterData.push(...bodyParams);
    }

    if (allParameterData.length === 0) {
      return {
        initParam: ts.factory.createIdentifier(DEFAULT_INIT_PARAM_NAME),
        packedParameters: ts.factory.createObjectLiteralExpression(),
        parameters: [
          ts.factory.createParameterDeclaration(
            undefined,
            undefined,
            DEFAULT_INIT_PARAM_NAME,
            ts.factory.createToken(ts.SyntaxKind.QuestionToken),
            ts.factory.createTypeReferenceNode(initTypeIdentifier),
          ),
        ],
      };
    }

    const parameterNames = allParameterData.map(([name]) => name);
    let initParamName = DEFAULT_INIT_PARAM_NAME;

    while (parameterNames.includes(initParamName)) {
      initParamName = `_${initParamName}`;
    }

    return {
      initParam: ts.factory.createIdentifier(initParamName),
      packedParameters: ts.factory.createObjectLiteralExpression(
        allParameterData.map(([name]) => ts.factory.createShorthandPropertyAssignment(name)),
      ),
      parameters: [
        ...allParameterData.map(([name, schema]) => {
          const nodes = new TypeSchemaProcessor(schema, this.#dependencies, this.#transferTypes).process();

          return ts.factory.createParameterDeclaration(
            undefined,
            undefined,
            name,
            undefined,
            ts.factory.createUnionTypeNode(nodes),
          );
        }),
        ts.factory.createParameterDeclaration(
          undefined,
          undefined,
          initParamName,
          ts.factory.createToken(ts.SyntaxKind.QuestionToken),
          ts.factory.createTypeReferenceNode(initTypeIdentifier),
        ),
      ],
    };
  }

  #extractParameterData(
    basicSchema?: OpenAPIV3.ReferenceObject | OpenAPIV3.SchemaObject,
  ): Array<readonly [string, Schema]> {
    if (!basicSchema) {
      return [];
    }

    const { logger, resolver } = this.#owner;

    const resolvedSchema = resolver.resolve(basicSchema);

    if (isObjectSchema(resolvedSchema) && !isEmptyObject(resolvedSchema)) {
      return Object.entries((resolvedSchema as NonEmptyObjectSchema).properties);
    }

    logger.warn("A schema provided for endpoint method's 'requestBody' is not supported");
    return [];
  }
}

import type Plugin from '@vaadin/hilla-generator-core/Plugin.js';
import PathManager from '@vaadin/hilla-generator-utils/dependencies/PathManager.js';
import ts, { type SourceFile } from 'typescript';

const CLIENT_TEMPLATE = `\
type RequestParams = Record<string, unknown>;

export interface ClientRequestInit {
  signal?: AbortSignal;
  headers?: Record<string, string>;
}

function createClient(baseUrl: string) {
  return {
    async call(
      method: string,
      path: string,
      params?: RequestParams,
      init?: ClientRequestInit,
    ): Promise<any> {
      let url = \`\${baseUrl}\${path}\`;
      const options: globalThis.RequestInit = {
        method,
        signal: init?.signal,
        headers: { ...init?.headers },
      };

      if (params) {
        const remaining: RequestParams = { ...params };

        // Replace path parameters
        for (const [key, value] of Object.entries(remaining)) {
          if (url.includes(\`{\${key}}\`)) {
            url = url.replace(\`{\${key}}\`, encodeURIComponent(String(value)));
            delete remaining[key];
          }
        }

        const hasRemaining = Object.keys(remaining).length > 0;

        if (hasRemaining && (method === 'GET' || method === 'DELETE' || method === 'HEAD')) {
          const searchParams = new URLSearchParams();
          for (const [key, value] of Object.entries(remaining)) {
            if (value !== undefined && value !== null) {
              searchParams.set(key, String(value));
            }
          }
          const qs = searchParams.toString();
          if (qs) {
            url += \`?\${qs}\`;
          }
        } else if (hasRemaining) {
          (options.headers as Record<string, string>)['Content-Type'] = 'application/json';
          options.body = JSON.stringify(remaining);
        }
      }

      const response = await fetch(url, options);

      if (!response.ok) {
        throw new Error(\`HTTP \${response.status}: \${response.statusText}\`);
      }

      if (response.status === 204 || response.headers.get('content-length') === '0') {
        return undefined;
      }

      const contentType = response.headers.get('content-type');
      if (contentType?.includes('application/json')) {
        return response.json();
      }

      return response.text();
    },
  };
}

const client = createClient('');
export default client;
`;

export default class ClientProcessor {
  readonly #owner: Plugin;
  readonly #outputPath: string;

  constructor(fileName: string, owner: Plugin) {
    this.#outputPath = new PathManager({ extension: 'ts' }).createRelativePath(fileName);
    this.#owner = owner;
  }

  process(): SourceFile {
    this.#owner.logger.debug(`Generating ${this.#outputPath}`);

    return ts.createSourceFile(this.#outputPath, CLIENT_TEMPLATE, ts.ScriptTarget.ES2021, true, ts.ScriptKind.TS);
  }
}

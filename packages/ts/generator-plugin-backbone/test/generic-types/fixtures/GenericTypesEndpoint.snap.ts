import type GenericTypesEntity_1 from "./com/vaadin/hilla/parser/plugins/backbone/generictypes/GenericTypesEntity.js";
import client_1 from "./connect-client.default.js";
import { ClientRequestInit as ClientRequestInit_1 } from "./connect-client.default.js";
async function unwrap_1(value: GenericTypesEntity_1<string | undefined>, init?: ClientRequestInit_1): Promise<string> { return client_1.call("POST", "/GenericTypesEndpoint/unwrap", { value }, init); }
async function wrap_1(value: string, init?: ClientRequestInit_1): Promise<GenericTypesEntity_1<string>> { return client_1.call("POST", "/GenericTypesEndpoint/wrap", { value }, init); }
export { unwrap_1 as unwrap, wrap_1 as wrap };

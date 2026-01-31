import type HierarchyRef_1 from "./com/vaadin/hilla/parser/plugins/backbone/hierarchyref/HierarchyRefEndpoint/HierarchyRef.js";
import client_1 from "./connect-client.default.js";
import { ClientRequestInit as ClientRequestInit_1 } from "./connect-client.default.js";
async function getHierarchyRef_1(data: Array<Record<string, string | undefined> | undefined> | undefined, init?: ClientRequestInit_1): Promise<HierarchyRef_1 | undefined> { return client_1.call("POST", "/HierarchyRefEndpoint/getHierarchyRef", { data }, init); }
export { getHierarchyRef_1 as getHierarchyRef };

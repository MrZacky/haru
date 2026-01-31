import client_1 from "../connect-client.js";
import { ClientRequestInit as ClientRequestInit_1 } from "../connect-client.js";
async function getString_1(init?: ClientRequestInit_1): Promise<string | undefined> { return client_1.call("POST", "/CustomClientEndpoint/getString", {}, init); }
export { getString_1 as getString };

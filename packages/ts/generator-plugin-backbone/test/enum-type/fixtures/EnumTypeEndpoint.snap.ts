import type EnumEntity_1 from "./com/vaadin/hilla/parser/plugins/backbone/enumtype/EnumTypeEndpoint/EnumEntity.js";
import client_1 from "./connect-client.default.js";
import { ClientRequestInit as ClientRequestInit_1 } from "./connect-client.default.js";
async function echoEnum_1(value: EnumEntity_1 | undefined, init?: ClientRequestInit_1): Promise<EnumEntity_1 | undefined> { return client_1.call("POST", "/EnumTypeEndpoint/echoEnum", { value }, init); }
async function echoListEnum_1(enumList: Array<EnumEntity_1 | undefined> | undefined, init?: ClientRequestInit_1): Promise<Array<EnumEntity_1 | undefined> | undefined> { return client_1.call("POST", "/EnumTypeEndpoint/echoListEnum", { enumList }, init); }
async function getEnum_1(init?: ClientRequestInit_1): Promise<EnumEntity_1 | undefined> { return client_1.call("POST", "/EnumTypeEndpoint/getEnum", {}, init); }
async function setEnum_1(value: EnumEntity_1 | undefined, init?: ClientRequestInit_1): Promise<void> { return client_1.call("POST", "/EnumTypeEndpoint/setEnum", { value }, init); }
export { echoEnum_1 as echoEnum, echoListEnum_1 as echoListEnum, getEnum_1 as getEnum, setEnum_1 as setEnum };

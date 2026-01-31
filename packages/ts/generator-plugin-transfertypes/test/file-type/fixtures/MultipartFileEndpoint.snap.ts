import client_1 from "./connect-client.default.js";
import { ClientRequestInit as ClientRequestInit_1 } from "./connect-client.default.js";
async function uploadFile_1(file: File | undefined, init?: ClientRequestInit_1): Promise<void> { return client_1.call("POST", "/MultipartFileEndpoint/uploadFile", { file }, init); }
export { uploadFile_1 as uploadFile };

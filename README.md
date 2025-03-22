# Extended TFTP Server and Client

## Overview
This project implements an extended **TFTP (Trivial File Transfer Protocol)** server and client. The TFTP server allows multiple users to upload, download, and delete files, as well as announce when files are added or removed.

The communication between the server and clients is performed using a **binary communication protocol** that supports file upload, download, and directory lookup. The server uses **bi-directional message passing**, meaning it can send messages to all logged-in users when needed.

The server follows a **Thread-Per-Client (TPC) architecture**, where each client connection is handled by a dedicated thread. The client operates with two threads:
- **Keyboard Thread** – Reads user input from the keyboard and sends corresponding packets to the server.
- **Listening Thread** – Reads incoming packets from the server, displays messages, and responds when necessary.

## Features
- User authentication with unique usernames.
- File upload and download.
- File deletion.
- Directory listing.
- Real-time notifications when files are added or deleted.
- Secure disconnection.

## Client Commands
The following commands can be typed into the terminal and sent by pressing Enter:

| Command  | Description |
|----------|------------|
| `LOGRQ <username>` | Log in to the server |
| `DELRQ <filename>` | Delete a file from the server |
| `RRQ <filename>` | Download a file from the server's "Files" folder to the current working directory |
| `WRQ <filename>` | Upload a file from the current working directory to the server |
| `DIRQ` | List all file names in the server's "Files" folder |
| `DISC` | Disconnect from the server and close the program |

## Protocol Messages
The extended TFTP protocol supports the following message types:

- **LOGRQ** – Login request  
- **RRQ / WRQ** – Read (download) / Write (upload) request  
- **DIRQ** – Directory listing request  
- **DATA** – Data packet  
- **ACK** – Acknowledgment packet  
- **DELRQ** – File deletion request  
- **BCAST** – Broadcast message for file addition/deletion  
- **ERROR** – Error message  
- **DISC** – Disconnect request  

## How to Run the Server and Client

### Running the Server
1. Navigate to the `server` folder.
2. Build the server:
   ``mvn compile```
Start the Thread-Per-Client server:
```mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpServer" ```

### Running the Client 
Navigate to the client folder.
Build the client:
``` mvn compile ``` </br>
Start the client:
``` mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpClient" -Dexec.args="127.0.0.1 7777" ```
(Use 127.0.0.1 as the IP address and 7777 as the port.)


 

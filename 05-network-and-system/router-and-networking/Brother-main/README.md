> **“风可以进，雨可以进，国王不能进。”**

欢迎使用：RSA消息非对称加密工具（哥哥科技）

下载、复制、存储、传输、操作、运行、分发或以任意方式使用该软件的二进制/源码或任何形式，即代表您**完全理解并同意、遵守[哥哥科技专有软件最终用户许可协议 (EULA)](https://github.com/ucxn/RSA-message-encrypt/blob/main/LICENSE.md)**

此工具可以加密文本，即使通讯全程被监听，也能确保消息的私密性（security）

警告：此工具无法防范中间人攻击（即对方将你发出去的密钥或者消息篡改）由于私钥只在本地存储，最有可能出现的攻击包括但不限于：篡改生成的公钥，使对方进行了不安全的加密，中间人攻击者解密你的消息后，重新用你的公钥进行加密，你甚至无法察觉；篡改发送的密文，使得其解码出不同的结果，由于公钥公开，任何人可用其加密，攻击者篡改你的密文消息后，仍可制造出有意义的信息，甚至无法察觉。

在攻击者单纯窃听而无法篡改双方的通信时，此工具是安全的。（不包括任何一方计算机中病毒等终端攻击，收集键盘敲击声音等旁信道攻击，直接冷冻内存的物理攻击，也不包括肩窥等社会工程学攻击）

如果处于复杂的网络攻击环境下，对此工具的安全性有担忧，可以使用多种不同的信道分批次的传输信息/校验，例如，用电话传递 Hash，使用Diffie-Hellman（ECDH），或者在线下交换CA证书。

本工具不加密 主RAM，请自行进行加密或者确保内存完整性；请自行确保计算机上没有可疑的高权限进程正在运行，也不存储或者运行任何*恶意软件* ；确保攻击者曾经没有物理攻击过计算机。本工具无法确保传输过程不被篡改，无法确保不发生拦截攻击或重放攻击，也不保证消息的真实性、完整性和不可抵赖性或隔离性、可控性、审计性和可用性。

虽然密码学的安全性不由加密算法本身的保密性保证，且开源的加密算法通常更安全，但本软件为了便于使用，对部分细节进行了一些修改，属于专有软件。请妥善保管好此软件，以避免将来无法解密。

本工具可以保证：在地球文明范围内，只要私钥绝对保密，密文即使完全、广泛泄露，能够保持至少114514年的离线安全性。即：无法从密文恢复明文，无法利用已知的明文和密文反推密码（密钥+口令）。

[作者：知乎](https://www.zhihu.com/people/guka0930)

功能：1. 加密 2.解密 3.生成密钥对 4.base64解密 0.指定密钥长度 H.消息认证码

Welcome to the RSA Asymmetric Encryption Tool

This tool can encrypt text and ensure the confidentiality of messages (security), even if the entire communication is being intercepted.

Warning: This tool cannot prevent man-in-the-middle (MITM) attacks (where the recipient's key or message is tampered with). Since the private key is stored locally, the most likely attacks include, but are not limited to: altering the generated public key, leading to insecure encryption on the recipient's side; a man-in-the-middle attacker decrypts your message, then re-encrypts it with your public key without your awareness; altering the encrypted message so that it decodes to a different result. Since the public key is available to anyone, attackers can tamper with your encrypted message and still create meaningful content, again without your knowledge.

This tool is secure when the attacker can only eavesdrop and cannot modify your communication (excluding terminal attacks such as computer viruses, side-channel attacks like recording keystrokes, physical attacks like freezing memory, or social engineering attacks like shoulder surfing).

If you are concerned about the security of this tool in a complex network attack environment, you can transmit/verify information in batches across different channels, such as sending a hash via phone, using Diffie-Hellman (ECDH), or exchanging CA certificates offline.

This tool does not encrypt RAM, so please encrypt it yourself or ensure memory integrity. Also, ensure no suspicious high-privilege processes are running on your computer and that no malware is present or active. Make sure the computer has never been physically compromised. This tool does not guarantee that the transmission is tamper-proof, nor can it prevent interception or replay attacks. It also does not guarantee message authenticity, integrity, non-repudiation, isolation, control, auditability, or availability.

Although cryptographic security does not rely on the secrecy of the encryption algorithm itself, and open-source algorithms are generally more secure, this software has made some modifications to certain details for ease of use, making it proprietary. Please keep this software safe to avoid future issues with decryption.

This tool guarantees that, within the realm of Earth’s civilization, as long as the private key is kept absolutely confidential, even if the ciphertext is fully and widely exposed, it will remain offline-secure for at least 114,514 years. That is, it is impossible to recover the plaintext from the ciphertext, or reverse-engineer the key (key + passphrase) from known plaintext and ciphertext.

Author: [https://www.zhihu.com/people/guka0930](https://www.zhihu.com/people/guka0930)


Features:  
1\. Encrypt  
2\. Decrypt  
3\. Generate key pair  
4\. Base64 decryption  
0\. Specify key length  
H. Message Authentication Code (HMAC)

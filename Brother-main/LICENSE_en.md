=
Brotech Proprietary Software End User License Agreement (EULA)
=======================================================================

**Software Name:** RSA Asymmetric Message Encryption Tool

**Program Format:** Interactive Terminal (TUI)

**Agreement Version:** 2.0 (Proprietary Binary Release)

**Release Date:** 2024

**Copyright © [2024-Present] Brotech. All Rights Reserved.**<br><br>

※ This translated version is for reference only. The [Simplified Chinese Version](./LICENSE.md) shall prevail and hold sole legal validity.<br><br>


**[IMPORTANT NOTICE — PLEASE READ CAREFULLY]**

This End User License Agreement (hereinafter referred to as "this Agreement") is a legal contract between you (a natural person, legal entity, or any unincorporated organization) and "Brotech" (hereinafter referred to as the "Author") regarding the aforementioned software product (hereinafter referred to as "this Software").

This Software is protected by copyright laws, international copyright treaties, and other intellectual property laws.

This Software is licensed for use, not sold. The Author grants you a limited, revocable, and non-sublicensable license to use this Software strictly in accordance with this Agreement; the ownership of this Software and its related intellectual property rights shall at all times remain with the Author.

By downloading, installing, copying, running, or otherwise accessing the binaries of this Software, you indicate that you have read, understood, and unconditionally agreed to be bound by all the terms and conditions of this Agreement.
If you do not agree to any of the terms of this Agreement, please do not download, install, or use this Software, and permanently destroy all copies of this Software from all your storage devices within 24 hours.

=======================================================================

Thank you for using this Software. This Software is a proprietary encryption tool based on a TUI (Text-based User Interface).<br>
The Author advocates for the pure interactive aesthetic of "The Tao of Simplicity." To maintain the original design philosophy and interactive experience of this Software,<br> by downloading, copying, distributing, or otherwise using the binary executable file of this Software, you fully agree to and accept all of the following terms:

### I. Scope of License

Subject to your compliance with all terms and conditions of this Agreement, the Author grants you the right to:

1. Use the official binary version of this Software released by the Author free of charge;
2. Copy and distribute the *official binary* files in their **original, unmodified** form;
3. Invoke this Software within your applications, workflows, or technical environments.

Anyone may freely use and distribute the official compiled binary version of this Software without restriction. The aforementioned license is limited strictly to the official binary version released by the Author. Except as expressly permitted by this Agreement, you may not claim any rights/powers beyond the scope granted herein. You may legally embed the binary files of this program into your applications or workflows, **provided that you strictly comply with the following provisions**.

### II. Integrity Protection and Prohibited Actions (Principle of Integrity)

This Software is provided only in compiled binary form; source code is not provided.

For any natural person, legal entity, organization, etc., excluding designated same-sex sworn brothers, the following acts are prohibited:

1. Decompiling, disassembling, reverse-engineering, cracking, dynamically analyzing, or otherwise attempting to derive the internal implementation details of this Software;
2. Modifying any content of this Software, including but not limited to:
* Executable files;
* Embedded text;
* Icons and resources;
* Data structures;
* Digital signatures;
* Checksum/Verification information;


3. Distributing any modified versions;
4. Impersonating, forging, or obfuscating the official release source.

**Strong Consistency Requirement:** The binary files you distribute must maintain byte-level consistency with the official release version. Any unauthorized modified versions shall not be distributed under the name of this Software.

When you distribute or reference the binary files of this Software, its file Hash value (e.g., SHA-256) and associated digital signatures must remain **100% identical** to the official version originally released by the Author. Any modification of a single byte is considered a severe infringement.

### III. Interface Interaction Restrictions (Principle of "As-Is" Presentation)

This software is purely a TUI (Text-based User Interface) program. This is a deliberate design choice by the author. Wrappers (GUI shells) and appearance tampering are strictly prohibited.

* You must not design any form of GUI (Graphical User Interface) to "wrap" or "shell" this program. This is not out of personal preference, but rather to avoid overcomplicating the technology stack.
* You must not, in any form, modify it into a CLI program that requires all arguments to be passed completely upfront. Typical characteristics of this include, but are not limited to: requiring users to master an operations manual beforehand. I will never merge such pull requests.
* You must not modify, obscure, or redirect the visual presentation of this program. If embedded into other programs, it must be presented to the end user **as-is and transparently**, using a different or the same terminal/console format.
* If this software is integrated into other programs, the visibility and transparency of the original terminal interactions should be maintained to the greatest extent possible.

\- Note: Retaining interactive text is to minimize dependency libraries and maintain minimalism. GUIs are excellent in principle, but are not being considered for this program at the moment. No one is allowed to create a wrapper unless an official one is released.<details>
  <summary>A CLI requiring complete upfront parameter passing is unacceptable.</summary>
Reasons include but are not limited to: Using graphical partitioning software is often more intuitive and safer than Diskpart; almost everyone chooses to preview Markdown after writing it; URL parameters (like those in OpenSpeedTest) do not conflict with a GUI — you can easily configure the parameters on the Web and then copy the URL, which in no way hinders automation and batch deployment. Command-line arguments are essentially just multiple-choice and boolean questions, so they can easily be transformed into interactive prompts, asking if you want to save the parameters before exiting. This way, all parameters are automatically generated by the program without any complex format conversions. The parser is also much simpler to write, eliminating the need for word frequency vectors to guess user intent or complex tokenizers, making it far more convenient for both developers and users. For scenarios with low bandwidth requiring SSH, a local GUI-to-CLI translator can easily be built. Tools are meant to serve people, and network maintainers should value elegance. For example, when subnetting IP ranges, one should focus on simplifying the underlying CIDR table entries; a good tool can calculate and determine this with one click, automatically converting the results into parameters. Even on rudimentary machines, the tool can be run on a mobile phone, utilizing OTG to simulate HID input. Technology exists to serve humanity. In short, a GUI completely encompasses CLI capabilities.</details>


### IV. Prohibition of Automated Arguments and Non-Interactive Invocation

The Author harbors a deep aversion to cumbersome Command Line Interface (CLI) arguments. This program is designed as one that "must be interactively inputted by a human."

* You **must not** write batch scripts (such as `.bat`, `.sh`, etc.) in an attempt to bypass the interactive logic.
* You **must not** transform this program into an automated tool capable of receiving silent external inputs via external redirection, pipes, keystroke simulations, or any form of parameter-passing mechanisms.
* You **must not** obscure or block in any way the standard output (stdout), standard error (stderr), or debugging information that this program is supposed to emit. Absolute transparency must be maintained while the program is running.

### V. Cross-Platform Compatibility Restrictions

This Software may be optimized for specific operating systems, terminal environments, or hardware architectures.

If developers or users require versions for different architectures, **they must not attempt to port or make drastic unauthorized modifications via compatibility layers**. Please contact the original Author directly for dedicated independent compilation and authorization.

### VI. Disclaimer of Warranty

**This Software is provided "AS IS", without any express or implied warranties.**
The Author makes no promises regarding the fitness, reliability, or error-free nature of this Software. In no event shall the Author be held liable for any direct, indirect, incidental, or special damages (including but not limited to data loss, business interruption, or privacy breaches) arising from the use or inability to use this Software. All risks associated with the use of this Software are borne solely by the user.

### VII. Distribution Restrictions

Silent distribution, bundling, and additional installations are strictly prohibited!

Any natural person, legal entity, or other organization, when copying, distributing, mirroring, hosting, redistributing, or otherwise providing the official binaries of this Software, shall not engage in the following acts:

1. Bundling, mixing, pre-installing, repackaging, or co-distributing this Software with any Adware, Malware, Spyware, cryptominers, browser hijackers, Potentially Unwanted Programs (PUPs), or other third-party programs that have no direct relation to the functionality of this Software;
2. Secondary packaging of this Software for distribution through Installers, Launchers, Downloaders, scripts, batch files, or other wrapping programs;
3. Deploying this Software or any additional components via silent installation, background installation, unattended installation, hidden installation, automatic launching, default checkmarks, forced bundled installation, deceptive installation, or other similar methods without the user's explicit knowledge and active consent;
4. Modifying, replacing, inserting, or attaching any third-party code, dynamic link libraries (DLLs), scripts, or services that affect this Software's original behavior, runtime environment, startup process, network behavior, or the user's system status;
5. Utilizing this Software to distribute any commercial promotional content, advertisements, statistical tracking modules, or remote download behaviors unrelated to the Author.

Any dissemination behavior violating these terms shall be deemed as unauthorized and illegal redistribution, and will not be recognized by the Author as an official release format. The Author reserves the right to terminate authorization immediately and pursue relevant liabilities.

---

## Additional Terms: Global Attribution Standards, Disclaimer of Warranty

These Additional Terms serve as global mandatory and recommended supplements to the license agreement for this project (including but not limited to scripts, code, programs, documents, images, text, videos, animations, workflows, and works in any other form).

Any entity using, modifying, or distributing this work must adhere to the following standards in any scenario where distributing this Software is permitted:

### 1. Core Attribution Identifier (Mandatory)

1. **Identifier Entity**: For the main or substantial portion of this program, attribution must explicitly use the text **"哥哥科技"** or **"Brotech"**.
2. **Character Set Compatibility**: In underlying environments supporting Chinese, including but not limited to Unicode (UTF-8/UTF-16), ANSI, GBK, or GB-18030, the text "哥哥科技" must be provided "as is". If restricted by extreme technical constraints, at least the text "Brotech" (composed entirely of ASCII characters) must be retained. No one may refuse attribution on the grounds of technical limitations.

### 2. Source Code Attribution Integrity (Mandatory)

1. Under all circumstances, all existing attribution formats in the Author's source code files (including but not limited to the text "哥哥科技" in comments and all related URL links) must be preserved intact.
2. Deleting, tampering with, or deliberately obfuscating the copyright notices built into the code is strictly prohibited.

### 3. Links and Platform Contact Standards

1. **Accompanying Links (Mandatory)**: Where technically feasible, attribution must be accompanied by the Author's exclusive link (e.g., [GitHub Repository Address](https://github.com/ucxn/RSA-message-encrypt)). Adding protocol headers (like `https://`) is permitted, but it is **strictly prohibited** to deliberately append incorrect port numbers, insert invisible characters, or execute any acts that substantially hinder user access.
2. **Multi-Platform Substitution Right (Optional)**: The aforementioned link may be replaced by any official homepage URL declared by the Author on Bilibili, Zhihu, Douban, Steam, GitHub, or in supplementary documentation.
3. **Platform-Specific Mentioning (Recommended)**: Using a platform's preferred contact or `@` mentioning mechanism (such as using UID on Bilibili or homepage URL on Zhihu) is encouraged. Adopting this mechanism can substitute the strict link requirement in Item 1 above, but the core principle of "maintaining a means of contact when technically possible" remains unchanged.
4. **Hypertext Adaptation (Advocated)**: Given rapid technological advancements, standardizing attribution using internationally recognized markup languages (such as Markdown's hyperlink syntax `[Brotech](URL)`) is encouraged.

### 4. Visual and Typography Standards (Mandatory)

1. **Visibility Guarantee**: In documents or UIs, the rendered size of the attribution text on the screen **must not be less than 6px**, or not smaller than **50%** of the regular font size used in the main body of that document. Hyperlinks may be hidden within plaintext, but invisible styling treatments (such as making the font color match the background, setting opacity to 0, etc.) are prohibited.
2. **Fair Display**: If a document has a dedicated references or acknowledgments section, moving the attribution there to avoid cluttering the main text is allowed. However, it must be granted the same visual treatment as other cited sources, in no particular order. Unfair "downgraded display" is absolutely prohibited.
3. **Artistic Interpretation and Machine Readability**:
* Displaying "Brotech" / "哥哥科技" using WordArt or images in a lawful manner conforming to public order and good morals is permitted.
* **Underlying Expression Consistency**: If using web/text WordArt, when a user selects, copies, and pastes it into internationally standard environments (like Notepad or search engines), it must be correctly parsed as the four standard Chinese characters "哥哥科技".
* **Graphic Interpretation Concession**: If image-based designs are used for attribution, the Author hereby grants permission for the two "哥" characters to feature different typographic designs. However, at least one "哥" character and the "科技" characters must be clearly legible and conform to Chinese language standards.
* **Accessibility Fallback**: If developers cannot guarantee the recognizability of their graphic designs, they must use internationally standard technical fallback methods (such as the `alt="哥哥科技"` attribute in HTML) to ensure the text information can be correctly scraped by machines and screen readers.



### 5. Disclaimer of Warranty

All works by the Author (including but not limited to scripts, code, programs, documents, etc.) are **provided "As-is"** and remain transparent to developers.

To the maximum extent permitted by applicable law, the Author provides no warranties, either express or implied, including but not limited to:

* Merchantability; Fitness for a particular purpose; Continuous availability; Error-free operation; Security; Data integrity.

The Author makes no direct or indirect guarantees regarding the fitness, security, or error-free nature of these programs. All risks arising from the use of this work are assumed solely by the user.

> [!CAUTION]
> This tool cannot defend against Man-In-The-Middle (MITM) attacks (i.e., where the adversary tampers with the keys or messages you transmit). Since private keys are stored locally, the most likely attacks include but are not limited to: Tampering with the generated public key, leading the other party to perform insecure encryption, whereupon the MITM attacker decrypts your message and re-encrypts it with your public key without you even noticing;<br>
> Tampering with the transmitted ciphertext, causing it to decode into a different result. Since the public key is public, (subject to technical constraints under extreme conditions) an attacker could tamper with your ciphertext to fabricate meaningful information without you noticing: The Author provides no guarantee against this.<br><br>
> This tool is secure when an attacker is purely eavesdropping and unable to tamper with your communication. (This does not include terminal attacks such as viruses on either party's computer, side-channel attacks like recording keystroke acoustics, physical attacks like freezing RAM, or social engineering attacks such as shoulder surfing).<br>
> If you are operating within a complex network attack environment and have concerns regarding the security of this tool, you may use multiple distinct channels to transmit/verify information in batches—for instance, relaying hashes via telephone, using Diffie-Hellman (ECDH), or exchanging CA certificates offline.<br><br>
> This tool does not encrypt RAM; please encrypt it yourself or ensure memory integrity. Please personally verify that no suspicious high-privilege processes are running on your computer, nor is any malware stored or executed. Ensure that the computer has not been subjected to prior physical attacks. This tool cannot ensure that the transmission process will not be tampered with, cannot prevent interception or replay attacks, and does not guarantee the authenticity, integrity, non-repudiation, isolation, controllability, auditability, or availability of messages.<br><br>
> Although the security of cryptography is not ensured by the secrecy of the encryption algorithm itself, and open-source encryption algorithms are generally safer, this Software has made modifications to certain details for ease of use, making it proprietary software. Please keep this Software safe to avoid being unable to decrypt your data in the future.

### 6. Severability

These Additional Terms are severable. If any provision of these rules, or any portion thereof, is ruled invalid or illegal in your jurisdiction, or irreconcilably conflicts with other open-source license terms you are obligated to follow, such invalidity or conflict **shall not affect the legal validity of any remaining provisions within these rules**, nor shall it affect the applicability of that invalid provision in other countries/regions.

### 7. Supplementary Provisions, Effectiveness, and Termination

#### 1. Termination

If you violate any terms of this Agreement, the Author reserves the right to terminate your authorization immediately.

Upon termination of the authorization, you must immediately cease all use of and delete all copies of this Software.

1. This Agreement becomes effective on the date you first access this Software and continues until terminated.
2. This license shall remain valid as long as you strictly adhere to all terms of this Agreement. If you fail to comply with any provision herein (especially terms concerning the prohibition of reverse-engineering, prohibition of automated shelling, and attribution standards), all rights granted to you under this Agreement shall **terminate immediately and automatically**, without further notice from the Author.
3. Upon termination of the Agreement, you must immediately cease using or distributing this Software in any form and completely destroy all binary copies of this Software and any of its derivative traces.
4. The Author reserves the right to unilaterally revoke the authorization for specific individuals or organizations to use this Software at any time and for any reason.

#### 2. All Rights Reserved

All rights not expressly granted to you in this license agreement are reserved by the Author. The intellectual property, core algorithm implementations, and proprietary designs of this Software belong solely to the original Author.

Associated with this Software:

* Source code; Algorithm implementations; Interaction design; Documentation; Brand identifiers; Technical architecture; all belong to the Author's exclusive intellectual property.

#### 3. Final Interpretation

The right of final interpretation regarding the underlying interaction design philosophy, agreement text, and related documentation of this Software belongs to "Brotech".<br><br> Any behavior attempting to bypass the textual expression of this Agreement or distort the Author's intent through legal loopholes will be deemed a malicious breach of contract.

=
                   [END OF AGREEMENT]
=======================================================================

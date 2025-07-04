
**Got questions, feedback, or feature requests? Join our community on [Slack!](https://join.slack.com/t/batfish-org/shared_invite/enQtMzA0Nzg2OTAzNzQ1LTcyYzY3M2Q0NWUyYTRhYjdlM2IzYzRhZGU1NWFlNGU2MzlhNDY3OTJmMDIyMjQzYmRlNjhkMTRjNWIwNTUwNTQ)**

[![codecov](https://codecov.io/gh/batfish/batfish/branch/master/graph/badge.svg)](https://codecov.io/gh/batfish/batfish)

## What is Batfish?

Batfish is a network validation tool Batfishはネットワーク検証ツールです that provides correctness guarantees for security, reliability, and compliance セキュリティ、信頼性、コンプライアンスの正確性保証を提供します by analyzing the configuration of network devices ネットワークデバイスの構成を分析します. It builds complete models of network behavior ネットワーク動作の完全なモデルを構築します from device configurations and finds violations of network policies  デバイスの構成から、ネットワークポリシーの違反を見つけます (built-in, user-defined, and best-practices).

A primary use case for Batfish バットフィッシュの主要なユースケース is to validate configuration changes *before* deployment 展開前に構成変更 *を検証することです (though it can be used to validate deployed configurations as well). Pre-deployment validation 展開前の検証 is a critical gap in existing network automation workflows 既存のネットワーク自動化ワークフローの重要なギャップです. By including Batfish in automation workflows, 自動化ワークフローにバットフィッシュを含めることにより network engineers can close this gap ネットワークエンジニアはこのギャップを埋めることができます and ensure that only correct changes are deployed 正しい変更のみが展開されるようにすることができます.

**Batfish does NOT require direct access to network devices.** The core analysis requires only the configuration of network devices. This analysis may be enhanced using additional information from the network such as:
* BGP routes received from external peers
* Topology information represented by LLDP/CDP

See [www.batfish.org](http://www.batfish.org) for technical information on how it works.

## What kinds of correctness checks does Batfish support?

[<img src=batfish_video.png width=370>](https://www.youtube.com/channel/UCA-OUW_3IOt9U_s60KvmJYA/videos)
[<img src=batfish_notebook.png width=470>](https://github.com/batfish/pybatfish/tree/master/jupyter_notebooks)

The [Batfish YouTube channel](https://www.youtube.com/channel/UCA-OUW_3IOt9U_s60KvmJYA/videos) (subscribe!) and [Python notebooks](https://github.com/batfish/pybatfish/tree/master/jupyter_notebooks) illustrate many checks. Batfish checks span a range of network behaviors.
#### Configuration Compliance　構成受け入れ
* Flag undefined-but-referenced 定義されていないが参照されるフラグ　or defined-but-unreferenced 定義されているが未使用フラグ structures (e.g., ACLs, route maps)
* Configuration settings for MTUs, AAA, NTP, logging, etc. match templates　テンプレートを一致させます
* Devices can only be accessed using SSHv2 デバイスはSSHV2 aを使用してのみアクセスできますand password is not null　パスワードは非NULL
#### Reliability 信頼性
* End-to-end reachability is not impacted エンドツーエンドの到達可能性は影響を受けません　for any flow どんな流れでも　after any single-link or single-device failure　単一リンクまたは単一デバイスの障害の後
* Certain services 特定のサービス　(e.g., DNS) are globally reachable　グローバルに到達可能
#### Security 安全
* Sensitive services can be reached only 敏感なサービスにのみ到達できます from specific subnets or devices 特定のサブネットまたはデバイスから
* Paths between endpoints are as expected エンドポイント間のパスは予想通りです (e.g., traverse a firewall, have at least 2 way ECMP, etc...)
#### Change Analysis 分析を変更します
* End-to-end reachability エンド間の到達可能性 is identical 同一です across the current and a planned configuration 現在および計画された構成全体で
* Planned ACL or firewall changes are provably correct 計画されたACLまたはファイアウォールの変更は、確かに正しいです　and causes no collateral damage for other traffic　(計画されているACLまたはファイアウォールの変更は、)他のトラフィックの付随的損害を引き起こしません
* Two configurations 2つの構成, potentially 潜在的に from different vendors, are functionally equivalent 機能的に同等です



## How do I get started?

#### 1. Run the Batfish service
Getting started with Batfish バットフィッシュを始めましょう is easy かんたんです. Just pull and run the latest `allinone` Docker container 最新の「Allinone」ドッカーコンテナを引っ張って実行するだけです　that includes Batfish as well as example Jupyter notebooks バットフィッシュとJupyterノートブックの例が含まれています. 

    docker pull batfish/allinone
   
    docker run --name batfish -v batfish-data:/data -p 8888:8888 -p 9997:9997 -p 9996:9996 batfish/allinone

The second command starts 2番目のコマンドは開始します the Batfish service and maps the necessary TCP ports バットフィッシュサービスと必要なTCPポートをマッピング.

##### Advanced Docker configuration:
The amount of memory available to Batfish is determined バットフィッシュが利用できるメモリの量が決定されます by the Docker configuration. You may wish to supply the [`--memory` command-line argument](https://docs.docker.com/config/containers/resource_constraints/#limit-a-containers-access-to-memory) --memoryを提供するのをおすすめします to explicitly set this value　この値を明示的に設定するため.

On Linux systems Linuxシステムで　that run the OOM Killer　OOMキラーを実行する, you may also wish to supply を提供するのをおすすめします　the `--oom-kill-disable` argument　, which runs in conjunction 組み合わせて実行します with the `--memory` argument to prevent Linux from killing Batfish when there is memory pressure on the system.

#### 2. Browse example notebooks (optional)

If you are new to Batfish, consider walking through our notebooks which highlight different capabilities and use cases of Batfish. Point your browser to [http://localhost:8888](http://localhost:8888), and in the `Password or token:` prompt, enter the token that Jupyter showed when you ran the container (e.g. **token=abcdef123456...**). 

Jupyter will show you the list of available notebooks. "Getting Started with Batfish" is a good one to start with. This [README](https://github.com/batfish/pybatfish/tree/master/jupyter_notebooks) explains what each notebook does. 


#### 3. Install Pybatfish

To analyze your network configurations, you also need [Pybatfish](https://www.github.com/batfish/pybatfish), a Python 3 SDK to interact with the Batfish service. Though not strictly necessary, we recommend that you install Pybatfish in a [virtual environment](https://docs.python.org/3/library/venv.html). 

To install Pybatfish run the following commands (in a virtual environment if applicable):

    python3 -m pip install --upgrade pybatfish

#### 4. Develop your analysis 

After installing Pybatfish, use your Python environment of choice (e.g., PyCharm, interactive Python shell, Jupyter, ..) to interact with Batfish. The [notebooks](https://github.com/batfish/pybatfish/tree/master/jupyter_notebooks) provide examples of such scripts. 

See complete documentation of Pybatfish on [readthedocs](https://pybatfish.readthedocs.io/en/latest/).


## System Requirements for running Batfish

Batfish can be run on any operating system that supports Docker. The containers are actively tested on Mac OS X and Ubuntu 16.04 LTS.

To get started with the example Jupyter notebooks, all you need is a reasonably capable laptop:

* Dual core CPU
* 8 GB RAM
* 256 GB hard-drive

When you transition to running Batfish on your own network, we recommend a server that at least has:

* Quad-core CPU with 2 threads per CPU
* 32 GB RAM
* 256 GB hard-drive


## Supported Network Device and Operating System List

Batfish supports configurations for a large and growing set of (physical and virtual) devices, including:

* A10 Networks
* Arista
* AWS (VPCs, Network ACLs, VPN GW, NAT GW, Internet GW, Security Groups, etc…)
* Cisco (All Cisco NX-OS, IOS, IOS-XE, IOS-XR and ASA devices)
* Check Point 
* [Cumulus](https://github.com/batfish/batfish/wiki/Packaging-snapshots-for-analysis#format-for-cumulus-configuration-files)
* [F5 BIG-IP](https://github.com/batfish/batfish/wiki/Packaging-snapshots-for-analysis#format-for-f5-big-ip-configuration-files)
* Fortinet
* Free-Range Routing (FRR)
* [iptables (on hosts)](https://github.com/batfish/batfish/wiki/Packaging-snapshots-for-analysis#format-for-host-json-files)
* Juniper (All JunOS platforms: MX, EX, QFX, SRX, T-series, PTX)
* Palo Alto Networks
* SONiC

Batfish has limited support for the following platforms:

* Aruba
* Dell Force10
* Foundry

If you'd like support for additional vendors or currently-unsupported configuration features, let us know via [Slack](https://join.slack.com/t/batfish-org/shared_invite/enQtMzA0Nzg2OTAzNzQ1LTcyYzY3M2Q0NWUyYTRhYjdlM2IzYzRhZGU1NWFlNGU2MzlhNDY3OTJmMDIyMjQzYmRlNjhkMTRjNWIwNTUwNTQ) or [GitHub](https://github.com/batfish/batfish/issues/new). We'll try to add support. Or, you can &mdash; we welcome pull requests! :)

## License and Dependencies

Batfish is released under The Apache Software License, Version 2.0. All
third-party dependencies are compatible with this licensing.

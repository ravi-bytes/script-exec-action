Secure Serverless Python SandboxA secure, isolated, and scalable environment for executing untrusted Python scripts, built on AWS Lambda and controlled via an Infrastructure as Code script using the AWS Serverless Application Model (SAM).This project is designed for applications (e.g., workflow builders like n8n, low-code platforms) that need to offer users the ability to run custom Python code on a server without compromising the security of the host system.🏛️ Architecture OverviewThis system is built on the principle of "isolate, execute, and destroy." Instead of running user code on a persistent server, we dynamically provision a secure, temporary micro-container (an AWS Lambda function) for each execution. This container has no network access and is destroyed the moment the script finishes.The entire infrastructure is defined in the template.yaml file and can be deployed with a single command.graph TD
    subgraph "Your Laptop / CI/CD"
        A[AWS SAM CLI] -- Deploys --> B((AWS CloudFormation));
        C[AWS CLI] -- Invokes --> E;
    end

    subgraph "AWS Cloud"
        B -- Creates/Updates --> D{Stack: secure-python-executor-stack};
        subgraph D
            subgraph "Isolated VPC (No Internet)"
                E[Lambda Function: workflow-python-executor];
            end
            F[IAM Role (Minimal Permissions)];
        end
        E -- Uses --> F;
    end

    style E fill:#FF9900,stroke:#333,stroke-width:2px
✨ Core Security FeaturesServerless Execution: No servers to manage, patch, or secure. Scales automatically.Complete Network Isolation: The script runs inside a VPC with no internet or internal network access, preventing data exfiltration or attacks on other resources.Strict IAM Permissions: The Lambda function runs with a "least-privilege" IAM role that only permits basic logging and VPC access.Ephemeral Environment: The execution environment is created on-demand and destroyed immediately after, ensuring no state persists between runs.Resource Limits: Strict memory (256MB) and time (30 seconds) limits prevent denial-of-service and infinite loop attacks.Read-Only Filesystem: The execution environment is read-only by default (except for /tmp), preventing the script from modifying its own environment.Infrastructure as Code (IaC): The entire security posture is defined in template.yaml, making it auditable, version-controlled, and repeatable.📂 Project Structure.
├── .github/
│   └── workflows/
│       └── deploy.yml        # GitHub Actions workflow for automated CI/CD deployment.
├── src/
│   └── lambda_function.py    # The Python code for the Lambda function handler.
└── template.yaml             # The AWS SAM script that defines all AWS resources.
File Purposetemplate.yaml: The Blueprint. This is the most important file. It uses the AWS Serverless Application Model (SAM) to define the isolated VPC, the secure IAM role, and the Lambda function with all its security configurations (timeout, memory, no-network, etc.).src/lambda_function.py: The Executor. This Python script is the heart of the Lambda function. It receives the user-submitted script as input, executes it safely using exec(), captures any output, and returns the result in a structured JSON format..github/workflows/deploy.yml: The Automator. This GitHub Actions workflow automates the deployment process. When you push changes to the main branch, it securely authenticates with AWS and runs the sam deploy command to update your infrastructure.🚀 Getting Started: Setup and DeploymentFollow these steps to deploy and test the project from your local machine.PrerequisitesEnsure you have the following tools installed and configured on your laptop:AWS CLI: Installation GuideAWS SAM CLI: Installation GuideDocker: Required by SAM to build deployment packages. Install Docker DesktopPython 3.12: It's best to have the same Python version locally as the Lambda runtime. Install via Homebrew.Step 1: Configure Local AWS CredentialsYour local machine needs credentials to communicate with AWS. We will use a secure, named profile.Create an IAM User: Follow the guide to create an access key for a new or existing IAM user in your AWS account. This user will need sufficient permissions to deploy CloudFormation stacks.Configure the Profile: Run the following command in your terminal and provide the credentials you just created.aws configure --profile workflow-app-dev
This will securely store your credentials in ~/.aws/credentials and your region settings in ~/.aws/config under the profile name workflow-app-dev.Step 2: Deploy the InfrastructureFor the first deployment, we'll use SAM's guided process.Build the Application: This command packages your Python code.sam build
Deploy with Guided Settings: This command will walk you through the initial deployment and save your settings for future use.sam deploy --guided --profile workflow-app-dev
When prompted:Stack Name: Enter a name like secure-python-executor-stack.AWS Region: Enter your preferred region (e.g., ap-south-1).Confirm changes before deploy: yAllow SAM CLI IAM role creation: ySave arguments to samconfig.toml: y (This is important!)SAM will now create all the necessary resources in your AWS account. This may take a few minutes.Note: For all subsequent deployments, you can simply run sam deploy --profile workflow-app-dev.Step 3: Execute a Test ScriptOnce deployment is complete, you can test the function from your laptop.Get the Function Name: Find the ExecutorFunctionName in the Outputs section of the sam deploy command's terminal output. It should be workflow-python-executor.Create a Sample Script: Create a file named my_script.py.# my_script.py
import platform
print(f"Hello from a secure, isolated sandbox!")
print(f"Running on Python version: {platform.python_version()}")
Prepare the Payload: The Lambda expects a JSON input. Create a payload.json file from your script. This command handles escaping and formatting.printf '{ "script": "%s" }' "$(cat my_script.py | sed 's/"/\\"/g' | tr -d '\n')" > payload.json
Invoke the Lambda: Run the invoke command, which executes the function in the cloud and saves the result locally.aws lambda invoke \
    --function-name "workflow-python-executor" \
    --cli-binary-format raw-in-base64-out \
    --payload file://payload.json \
    --profile workflow-app-dev \
    --region ap-south-1 \
    response.json
View the Result: The output from the Lambda is saved in response.json.cat response.json
For a cleaner view of just your script's output, use jq:cat response.json | jq -r '.body' | jq -r '.result'
Expected Output:Hello from a secure, isolated sandbox!
Running on Python version: 3.12.x
You have now successfully deployed, executed, and received a result from your secure serverless sandbox!


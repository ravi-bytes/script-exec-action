# src/lambda_function.py
import json
import sys
from io import StringIO

def lambda_handler(event, context):
    """
    Executes a user-provided Python script in a secure manner.
    """
    try:
        user_script = event['script']
        script_globals = {}
        
        original_stdout = sys.stdout
        sys.stdout = captured_output = StringIO()

        exec(user_script, script_globals)

        sys.stdout = original_stdout
        result = captured_output.getvalue()

        return {
            'status': 'success',
            'result': result
        }

    except Exception as e:
        return {
            'status': 'error',
            'error_message': str(e)
        }

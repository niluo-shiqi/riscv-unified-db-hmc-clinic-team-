"""
Script to convert .udb files to .yml files and vice versa.
"""
import sys
import re

# String fields that should always have quotes around them
QUOTED_FIELDS = [
    "$schema",
    "version",
]

# String fields that don't have quotes around them in YAML
STRING_FIELDS = [
    # General fields
    "name",
    "long_name",
    "description",
    "$source",  # TODO: check if this already has quotes
    "id",
    "url",
    "text_url",
    "email",
    "introduction",

    # CSR fields
    "alias",

    # Instruction fields
    "assembly",
    "operation_ast",
    "access_detail",
    "match",
    "$child_of",  # TODO: check

    # Extension fields
    "rvi_jira_issue",
    "branch",
    "company"

    # IDL fields
    "sw_read()",
    "reset_value()",
    "sw_write(csr_value)",
    "legal?(csr_value)",
    "type()",
    "operation()",
]

# Fields that are yaml arrays of strings
YAML_ARRAY_STRING_FIELDS = [
    # CSR fields
    "items", # for alias

    # Instruction fields
    "$inherits"
    "hints",
    
    # Extension fields
    "changes",
]

# Fields that are arrays of strings (i.e., denoted with square brackets)
ARRAY_STRING_FIELDS = [
    "affectedBy",
]


def convert_udb_to_yaml(udb_file):
    """Conversion from YAML to UDB involves removing quotes around string values"""

    with open(udb_file, "r") as file:
        lines = file.readlines()

    output_lines = []
    for line in lines:
        # Don't process comments
        if line.startswith("#"):
            output_lines.append(line)

        else:
            try:
                field, value = line.split(":", 1)

                # Pre-process field string before checking if it's in QUOTED_FIELDS
                clean_field = field.strip()
                if clean_field.startswith('-'):
                    clean_field = clean_field[1:].strip()

                # Don't remove quotes for quoted fields
                if clean_field.strip() in QUOTED_FIELDS:
                    output_lines.append(line)
                else:
                    # Remove quotes unless they're escaped
                    value = re.sub(r'(?<!\\)"', '', value)

                    # Remove '\' from escaped quotes
                    value = value.replace('\\"', '"')

                    output_lines.append(f"{field}:{value}")

            # Line doesn't have ":"
            except ValueError:
                # Remove quotes unless they're escaped
                line = re.sub(r'(?<!\\)"', '', line)

                # Remove '\' from escaped quotes
                line = line.replace('\\"', '"')
                
                output_lines.append(line)

    output_file = udb_file.rsplit(".", 1)[0] + ".yaml"
    with open(output_file, "w") as file:
        file.writelines(output_lines)


def convert_yaml_to_udb(yaml_file):
    """Conversion from YAML to UDB involves adding quotes around string values"""

    with open(yaml_file, "r") as file:
        lines = file.readlines()

    output_lines = []
    inMultiLineString = False   # to keep track if we're currently processing a multi-line string
    inYamlStringArray = False       # to keep track if we're currently processing an array of strings
    inHintsArray = False        # to keep track if we're currently processing the hints array, which has special formatting
    for i, line in enumerate(lines):
        # Don't process comments or empty lines
        if line.startswith("#") or line.strip() == "":
            output_lines.append(line)

        else:
            # Check if the line is part of a multi-line string
            if inMultiLineString:
                lineIndentation = len(re.search(r'^\s*', line).group())

                if (lineIndentation > indentation) or line.strip() == "":
                    # escape quotes
                    line = line.replace('"', '\\"')
                    
                    # edge case for when a multi-line string is at the end of a file
                    if i == len(lines) - 1:
                        line = line + "\"\n"
                    
                    output_lines.append(line)
                    continue
                
                # when we reach a line that has less indentation, we know the
                # multi-line string has ended, so add a closing quote
                else:
                    inMultiLineString = False
                    line = (" " * indentation) + "\"\n" + line
            
            # Check if the line is part of an array of strings
            elif inYamlStringArray:
                if line.strip().startswith("-"):
                    # escape quotes
                    line = line.replace('"', '\\"')

                    # hints has {} around the portion that's a string
                    if inHintsArray:
                        line = re.sub(r'(-\s*)\{(\s*)(.*?)(\s*)\}', r'\1{\2"\3"\4}', line)
                    else:
                        # add quotes around the string value
                        line = re.sub(r'(-\s*)(.*)', r'\1"\2"', line)
                    
                    output_lines.append(line)
                    
                    continue
                else:
                    inYamlStringArray = False

            try:
                field, value = line.split(":", 1)
                value = value.strip()

                # Handle multi-line strings
                if value.startswith("|"):
                    inMultiLineString = True

                    # Get indentation level to determine 
                    # when the multi-line string ends
                    indentation = len(re.search(r'^\s*', field).group())

                    output_lines.append(f"{field}: {value} \"\n")
                    continue
                
                # Add quotes around string values
                if field.strip() in STRING_FIELDS:
                    value = f'"{value}"' if value else value

                # Add quotes around quoted fields if they don't already have quotes
                elif field.strip() in QUOTED_FIELDS:
                    if not (value.startswith('"') and value.endswith('"')):
                        value = f'"{value}"' if value else value

                elif field.strip() in YAML_ARRAY_STRING_FIELDS:
                    inYamlStringArray = True
                    inHintsArray = field.strip() == "hints"
                
                elif field.strip() in ARRAY_STRING_FIELDS:
                    # Add quotes around each element in the array
                    elements = [elem.strip() for elem in value.strip("[]").split(",")]
                    quoted_elements = [f'"{elem}"' for elem in elements]
                    value = f"[{', '.join(quoted_elements)}]"

                # Opcode itself isn't a string, but it contains a string that needs quotes
                elif field.strip() == "opcode":
                    nested_field, nested_value = value[1:-1].strip().split(":", 1)
                    nested_value = nested_value.strip()
                    nested_value = f'"{nested_value}"'
                    value = f"{{ {nested_field}: {nested_value} }}"
                
                output_lines.append(f"{field}: {value}\n")

            except ValueError:
                # When the line doesn't contain a ":"
                output_lines.append(line)


    output_file = yaml_file.rsplit(".", 1)[0] + ".udb"
    with open(output_file, "w") as file:
        file.writelines(output_lines)


if __name__ == "__main__":
    # Get CLI arguments
    if len(sys.argv) == 2:
        INPUT_FILE = sys.argv[1]  # .udb file or .yml file to convert
    else:
        print("USAGE: python convertudb.py [INPUT]")
        print("where INPUT is either a .udb file or a .yaml file to convert to the other format.")
        sys.exit()

    if INPUT_FILE.endswith(".udb"):
        convert_udb_to_yaml(INPUT_FILE)
    elif INPUT_FILE.endswith(".yml") or INPUT_FILE.endswith(".yaml"):
        convert_yaml_to_udb(INPUT_FILE)
    else:
        raise ValueError("INPUT must be either a .udb file or a .yaml file.")

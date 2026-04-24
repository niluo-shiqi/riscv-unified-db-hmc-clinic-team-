"""
Script to convert .udb files to .yml files and vice versa.

NOTE: doesn't handle in-line comments, assumes that all comments exist
      on their own line (TODO: add support for in-line comments)
"""
import sys
import re

# String fields that should always have quotes around them
QUOTED_FIELDS = [
    "version",
]

# String fields that don't have to have quotes around them in YAML
STRING_FIELDS = [
    # General fields
    "$schema",
    "name",
    "long_name",
    "description",
    "$source",
    "id",
    "url",
    "text_url",
    "email",
    "introduction",

    # CSR fields
    "alias",
    "requires",

    # Instruction fields
    "assembly",
    "operation_ast",
    "access_detail",
    "match",
    "$child_of",
    "display_name",
    "parent_of",

    # Extension fields
    "rvi_jira_issue",
    "branch",
    "company",

    # Profile fields
    "text",
    "note",
    "$parent_of",
    "marketing_name",

    # Register file fields
    "register_class",
    "arch_read()",
    "arch_write(value)",

    # IDL fields
    "sw_read()",
    "reset_value()",
    "sw_write(csr_value)",
    "legal?(csr_value)",
    "type()",
    "operation()",

    # Conditions
    "reason",
    "equal",

    # TODO: requires not
]

# Fields that aren't strings, but have strings in them
# e.g. release: { $ref: "releaseAddress" }
HAS_STRINGS = [
    "opcode",
    "release",
    "not",
]

# Fields that are yaml arrays of strings
YAML_ARRAY_STRING_FIELDS = [
    "doc_links",

    # CSR fields
    "items", # for alias
    
    # Extension fields
    "changes",
]

# Fields that are yaml arrays of values that contain strings
# e.g. an element in "hints" looks like 
#    - { $ref: "inst/Zihintntl/c.ntl.p1.yaml#" }
YAML_ARRAY_HAS_STRINGS = [
    "hints",
]

# Fields that are arrays of strings, but without the '-' prefixing every element
YAML_LIST_STRING_FIELDS = [
    "fields",
    "opcodes",
    "extensions",
]

# Fields that can be EITHER a yaml array of strings or just a string
YAML_ARRAY_OR_STRING_FIELDS = [
    "$inherits",
    "$remove",
]

# Fields that are arrays of strings (i.e., denoted with square brackets)
ARRAY_STRING_FIELDS = [
    "affectedBy",
    "abi_mnemonics",
]

KEYWORDS = (STRING_FIELDS + YAML_ARRAY_STRING_FIELDS + HAS_STRINGS + 
            YAML_ARRAY_OR_STRING_FIELDS + YAML_LIST_STRING_FIELDS +
            YAML_ARRAY_HAS_STRINGS)

def add_quotes(s):
    """Add quotes to string s if it doesn't already have them"""
    if not (s.startswith('"') and s.endswith('"')):
        return f'"{s}"' if s else s
    return s

def add_nested_quotes(s):
    try:
        nested_field, nested_value = s[1:-1].strip().split(":", 1)
        nested_value = nested_value.strip()
        nested_value = add_quotes(nested_value)
        return f"{{ {nested_field}: {nested_value} }}"
    
    # 'not' has ambiguity, this handles when 'not' doesn't
    # have any strings that need to be quoted
    except ValueError:
        return s

def convert_udb_to_yaml(udb_file):
    """Conversion from YAML to UDB involves removing quotes around string values"""

    with open(udb_file, "r") as file:
        lines = file.readlines()

    output_lines = []
    for line in lines:
        # Don't process comments
        if line.strip().startswith("#"):
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

                    # Remove '\' from escaped backslashes
                    value = value.replace('\\\\', '\\')

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
    inYamlStringArray = False   # to keep track if we're currently processing an array of strings
    inYamlStringList = False    # to keep track if we're currently processing a list of strings
    inArrayHasStrings = False   # to keep track if we're currently processing an array that isn't
                                #  a list of strings, but has strings in the elements
    for i, line in enumerate(lines):
        
        # Don't process comments or empty lines
        stripped = line.strip()
        if stripped.startswith("#") or stripped == "":
            output_lines.append(line)

        else:
            # Check if the line is part of a multi-line string
            if inMultiLineString:
                lineIndentation = len(re.search(r'^\s*', line).group())

                if (lineIndentation > multiline_indentation) or line.strip() == "":
                    # escape backslashes
                    line = line.replace('\\', '\\\\')

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
                    output_lines.append(" " * multiline_indentation + "\"\n")
            
            # Check if the line is part of an array of strings
            if inYamlStringArray:
                if line.strip().startswith("-"):
                    prefix, value = line.split("-", 1)

                    # some arrays have {} around the portion that's a string
                    if inArrayHasStrings:
                        value = add_nested_quotes(value.strip())

                    else:
                        # add quotes around the string value
                        value = add_quotes(value.strip())
                    
                    line = f"{prefix}- {value}\n"
                    output_lines.append(line)
                    
                    continue
                else:
                    inYamlStringArray = False
                    inArrayHasStrings = False
            
            # Check if line is part of a YAML String list
            if inYamlStringList:
                lineIndentation = len(re.search(r'^\s*', line).group())

                # Check that a field isn't already a keyword
                field = line.split(':', 1)[0].strip()
                if field in KEYWORDS:
                    pass

                # Add quotes around fields in the list
                elif (lineIndentation == field_indentation) or line.strip() == "":
                    field_name = line.strip().split(":", 1)[0]
                    
                    output_lines.append(" " * field_indentation +f'"{field_name}":\n')
                    continue
                elif lineIndentation < field_indentation:
                    inYamlStringList = False


            try:
                field, value = line.split(":", 1)
                value = value.strip()

                # Handle multi-line strings
                if value.startswith("|"):
                    inMultiLineString = True

                    # Get indentation level to determine 
                    # when the multi-line string ends
                    multiline_indentation = len(re.search(r'^\s*', field).group())

                    output_lines.append(f"{field}: {value} \"")

                    # Edge case for when a multiline string is empty and at the end of the file
                    if i == len(lines) - 1:
                        output_lines[-1] += "\""

                    output_lines[-1] += "\n"

                    continue
                
                # Pre-process field string before checking if it's value needs quotes
                clean_field = field.strip()
                if clean_field.startswith('-'):
                    clean_field = clean_field[1:].strip()
                
                # Handle fields that can be either a plain string or an array of strings
                if clean_field in YAML_ARRAY_OR_STRING_FIELDS:
                    # The value is a YAML array
                    if value == "":
                        inYamlStringArray = True
                    
                    # The value is just a string
                    else:
                        value = add_quotes(value)
                
                # Add quotes around string values if not already quoted
                elif clean_field in STRING_FIELDS:
                    value = add_quotes(value)

                # Add quotes around quoted fields if they don't already have quotes
                elif clean_field in QUOTED_FIELDS:
                    value = add_quotes(value)

                elif clean_field in (YAML_ARRAY_STRING_FIELDS + YAML_ARRAY_HAS_STRINGS):
                    inYamlStringArray = True
                    inArrayHasStrings = field.strip() in YAML_ARRAY_HAS_STRINGS

                elif clean_field in YAML_LIST_STRING_FIELDS and value == "":
                    inYamlStringList = True
                    field_indentation = len(re.search(r'^\s*', lines[i+1]).group())
                
                elif clean_field in ARRAY_STRING_FIELDS:
                    # Add quotes around each element in the array
                    elements = [elem.strip() for elem in value.strip("[]").split(",")]
                    quoted_elements = [add_quotes(elem) for elem in elements]
                    value = f"[{', '.join(quoted_elements)}]"

                # Fields that aren't strings, but contain a strings that needs quotes
                elif clean_field in HAS_STRINGS:
                    value = add_nested_quotes(value)

                # equal could be either an int, boolean, or a string
                elif clean_field == "equal":
                    if not (value.isnumeric() or (value.lower() in ("true", "false"))):
                        value = add_quotes(value)
                
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

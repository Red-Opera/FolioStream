package com.springboot.model;

import java.util.List;

public class ApiReference {
    private String name;
    private String type;
    private String description;
    private String category;
    private List<ApiMethod> methods;
    private List<ApiProperty> properties;
    private List<String> examples;

    public ApiReference() {}

    public ApiReference(String name, String type, String description, String category, 
                       List<ApiMethod> methods, List<ApiProperty> properties, List<String> examples) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.category = category;
        this.methods = methods;
        this.properties = properties;
        this.examples = examples;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<ApiMethod> getMethods() { return methods; }
    public void setMethods(List<ApiMethod> methods) { this.methods = methods; }

    public List<ApiProperty> getProperties() { return properties; }
    public void setProperties(List<ApiProperty> properties) { this.properties = properties; }

    public List<String> getExamples() { return examples; }
    public void setExamples(List<String> examples) { this.examples = examples; }

    // Inner classes for method and property definitions
    public static class ApiMethod {
        private String name;
        private String returnType;
        private String description;
        private List<ApiParameter> parameters;
        private String example;

        public ApiMethod() {}

        public ApiMethod(String name, String returnType, String description, 
                        List<ApiParameter> parameters, String example) {
            this.name = name;
            this.returnType = returnType;
            this.description = description;
            this.parameters = parameters;
            this.example = example;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<ApiParameter> getParameters() { return parameters; }
        public void setParameters(List<ApiParameter> parameters) { this.parameters = parameters; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }

        public String getSignature() {
            StringBuilder signature = new StringBuilder();
            signature.append(returnType).append(" ").append(name).append("(");
            
            if (parameters != null && !parameters.isEmpty()) {
                for (int i = 0; i < parameters.size(); i++) {
                    ApiParameter param = parameters.get(i);
                    signature.append(param.getType()).append(" ").append(param.getName());
                    if (i < parameters.size() - 1) {
                        signature.append(", ");
                    }
                }
            }
            
            signature.append(")");
            return signature.toString();
        }
    }

    public static class ApiParameter {
        private String name;
        private String type;
        private String description;
        private boolean required;

        public ApiParameter() {}

        public ApiParameter(String name, String type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }

    public static class ApiProperty {
        private String name;
        private String type;
        private String description;
        private boolean readOnly;

        public ApiProperty() {}

        public ApiProperty(String name, String type, String description, boolean readOnly) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.readOnly = readOnly;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isReadOnly() { return readOnly; }
        public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    }
}
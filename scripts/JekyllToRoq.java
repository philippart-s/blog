//JAVA 25

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final static String JEKYLL_BLOG_PATH = "./_posts/";
final static String ROQ_BLOG_PATH = "../content/posts/tmp/";


private String transformJekyllToRoq(String content, String targetFolder) {
    // Front matter cpature
    Pattern frontMatterPattern = Pattern.compile("^---(.*?)---", Pattern.DOTALL);
    Matcher matcher = frontMatterPattern.matcher(content);

    if (matcher.find()) {
        String jekyllFrontMatter = matcher.group(1);
        String body = content.substring(matcher.end()).trim();

        body = body.replaceAll("<meta content=\"\\{\\{.*?property=\"og:image\">", "");

        String headOfBody = body.substring(0, Math.min(body.length(), 300));

        Pattern imageOnlyPattern = Pattern.compile("^!\\[.*?\\]\\(.*?/assets/images/([^\\s\\)]+).*?\\)(?:\\{:.*?\\})*", Pattern.MULTILINE);
        Matcher imageMatcher = imageOnlyPattern.matcher(headOfBody);

        String imageForFrontMatter = "";
        String figCaption = "";


        // Images manipulations
        if (imageMatcher.find()) {
            String fullImagePath = imageMatcher.group(1);
            imageForFrontMatter = Paths.get(fullImagePath).getFileName().toString();
            String imageFullMatch = imageMatcher.group(0);

            // Delete header image
            body = body.replace(imageFullMatch, "").trim();

            // Get copyright
            Pattern linkPattern = Pattern.compile("^\\[(.*?)\\]\\(.*?\\)(?:\\{:.*?\\})*", Pattern.DOTALL);
            Matcher linkMatcher = linkPattern.matcher(body);

            if (linkMatcher.find()) {
                figCaption = linkMatcher.group(1);
                IO.println("figCaption: " + figCaption);

                body = body.replace(linkMatcher.group(0), "").trim();

                if (body.startsWith("<br/>")) {
                    body = body.substring(5).trim();
                }
            }
        }


        // Remove Kramedown code
        body = body.replace("{:target=\"_blank\"}", "");

        // Update path images
        body = body.replaceAll("!\\[(.*?)\\]\\(.*?/assets/images/.*?/([^\\s\\)]+).*?\\)(?:\\{:.*?\\})*", "![$1]($2)");

        // Remove Kramedown code
        body = body.replaceAll("\\{:.*?\\}", "");

        // Do not touch to Qute code
        body = protectRoqExpressions(body);

        // Jekyll to ROQ data transfert
        String title = extractValue(jekyllFrontMatter, "title");
        String description = extractValue(jekyllFrontMatter, "excerpt");
        String tagsBlock = extractRawYamlList(jekyllFrontMatter, "tags");

        // New front matter
        StringBuilder roqFront = new StringBuilder("---\n");
        roqFront.append("title: \"").append(title).append("\"\n");
        roqFront.append("description: \"").append(description.replace("\"", "\\\"")).append("\"\n");
        roqFront.append("link: /").append(targetFolder).append("\n");
        roqFront.append("tags: \n").append(tagsBlock).append("\n");
        if (!imageForFrontMatter.isEmpty()) {
            roqFront.append("image: ").append(imageForFrontMatter.isEmpty() ? "image-illustration.jpg" : imageForFrontMatter).append("\n");
            roqFront.append("figCaption: \"").append(figCaption.isEmpty() ? "@wildagsx" : figCaption).append("\"\n");
        }
        roqFront.append("author: wilda\n");
        roqFront.append("---\n\n");

        return roqFront + body;
    }
    return content;
}

private String extractValue(String yaml, String key) {
    // We capture the value after the key, ignoring the surrounding quotation marks if they exist.
    Pattern p = Pattern.compile(key + ":\\s*(.*)(?:\\n|$)");
    Matcher m = p.matcher(yaml);
    if (m.find()) {
        String value = m.group(1).trim();
        // On retire les guillemets de début et de fin s'ils sont présents dans Jekyll
        return value.replaceAll("^\"|\"$|^'|'$", "").trim();
    }
    return "";
}
private String extractRawYamlList(String yaml, String key) {
    // Capture the entire block that starts with “-” after the specified key.
    Pattern p = Pattern.compile(key + ":\\s*\\n((?:\\s*-.*?(?:\\n|$))+)");
    Matcher m = p.matcher(yaml);
    if (m.find()) {
        return m.group(1).stripTrailing();
    }
    return "  - blog";
}

private String extractFirstImage(String body) {
    // Search for ![alt text](url) and extract the filename.
    Pattern p = Pattern.compile("!\\[.*?\\]\\(.*?/([^/\\)]+\\.(?:jpg|png|jpeg|webp))\\)");
    Matcher m = p.matcher(body);
    return m.find() ? m.group(1) : "";
}

private String extractFigCaption(String body) {
    // Search for credit links like [© Pauline].
    Pattern p = Pattern.compile("\\[(©.*?)\\]");
    Matcher m = p.matcher(body);
    return m.find() ? m.group(1) : "";
}

private String extractImageSourceSubPath(String body) {
    // Capture the part after /assets/images/ (e.g., javelit/gondola.jpg).
    Pattern p = Pattern.compile("/assets/images/([^\\s\\)]+\\.(?:jpg|png|jpeg|webp|svg))");
    Matcher m = p.matcher(body);
    return m.find() ? m.group(1) : "";
}

private String protectRoqExpressions(String body) {
    // We only protect it if the brace is immediately followed by a letter,
    // with no space, which corresponds to Qute/Roq expressions.
    // We add a space: {events → { events.
    return body.replaceAll("\\{([a-zA-Z])", "{ $1");
}

void main() {

    Path jekyllPath = Paths.get(JEKYLL_BLOG_PATH);
    Path roqPath = Paths.get(ROQ_BLOG_PATH);
    Path jekyllImagesBase = Paths.get(jekyllPath + "/images/");
    IO.println(jekyllImagesBase);

    // First step: files migration
    try (var files = Files.list(jekyllPath)) {
        files.filter(path -> path.toString().endsWith(".md"))
                .forEach(jekyllFile -> {
                    try {
                        // 1. Determine the folder name (e.g., 2025-01-12-title).
                        String fileName = jekyllFile.getFileName().toString();
                        String folderName = fileName.replace(".md", "");

                        // 2. Create the destination folder.
                        Path targetFolder = roqPath.resolve(folderName);
                        Files.createDirectories(targetFolder);

                        // 3. Define the target file index.md.
                        Path targetFile = targetFolder.resolve("index.md");

                        // 4. Copy the content.
                        String content = Files.readString(jekyllFile);
                        String imageSourceSubPath = extractImageSourceSubPath(content);

                        if (!imageSourceSubPath.isEmpty()) {
                            Path sourceImagePath = jekyllImagesBase.resolve(imageSourceSubPath);
                            if (Files.exists(sourceImagePath)) {
                                Files.copy(sourceImagePath, targetFolder.resolve(sourceImagePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }

                        String transformedContent = transformJekyllToRoq(content, targetFolder.getName((targetFolder.getNameCount() != 0 ? targetFolder.getNameCount() - 1 : 0)).toString());
                        Files.writeString(targetFile, transformedContent);
                        IO.println("Migré : " + fileName + " -> " + folderName + "/index.md");
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la migration de " + jekyllFile + " : " + e.getMessage());
                    }
                });
    } catch (IOException e) {
        e.printStackTrace();
    }

}

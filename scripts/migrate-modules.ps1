$ErrorActionPreference = "Stop"
$root = "c:\Users\lunga\OneDrive\Desktop\ALIS_BACKEND-main - Copy"
$src = Join-Path $root "src\main\java\za\ac\alis"
$srcSeed = Join-Path $root "src\main\java\za\ac\seed"
$srcRes = Join-Path $root "src\main\resources"

function Copy-JavaFile($sourceFile, $destFile, $packageFrom, $packageTo) {
    $dir = Split-Path $destFile -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $content = Get-Content $sourceFile -Raw -Encoding UTF8
    $content = $content -replace [regex]::Escape("package $packageFrom;"), "package $packageTo;"
    Set-Content -Path $destFile -Value $content -Encoding UTF8 -NoNewline
}

function Apply-ImportMap($filePath) {
    $content = Get-Content $filePath -Raw -Encoding UTF8
    $replacements = [ordered]@{
        'import za.ac.alis.entities.' = 'import za.ac.alis.core.persistence.'
        'import za.ac.alis.enums.' = 'import za.ac.alis.core.enums.'
        'import za.ac.alis.dto.' = 'import za.ac.alis.core.dto.'
        'import za.ac.alis.projections.' = 'import za.ac.alis.core.projections.'
        'import za.ac.alis.queries.' = 'import za.ac.alis.core.queries.'
        'import za.ac.alis.utils.' = 'import za.ac.alis.core.util.'
        'import za.ac.alis.repo.ClientRepository' = 'import za.ac.alis.user.persistence.ClientRepository'
        'import za.ac.alis.repo.AdminRepository' = 'import za.ac.alis.user.persistence.AdminRepository'
        'import za.ac.alis.repo.LegalPractitionerRepository' = 'import za.ac.alis.user.persistence.LegalPractitionerRepository'
        'import za.ac.alis.repo.DealMakerRepository' = 'import za.ac.alis.user.persistence.DealMakerRepository'
        'import za.ac.alis.repo.DocumentRepository' = 'import za.ac.alis.legal.persistence.DocumentRepository'
        'import za.ac.alis.repo.DocumentContentRepository' = 'import za.ac.alis.legal.persistence.DocumentContentRepository'
        'import za.ac.alis.repo.FileMetadataRepository' = 'import za.ac.alis.legal.persistence.FileMetadataRepository'
        'import za.ac.alis.repo.SummaryReportRepository' = 'import za.ac.alis.legal.persistence.SummaryReportRepository'
        'import za.ac.alis.repo.LawRuleRepository' = 'import za.ac.alis.legal.persistence.LawRuleRepository'
        'import za.ac.alis.repo.ClauseRepository' = 'import za.ac.alis.legal.persistence.ClauseRepository'
        'import za.ac.alis.repo.AuditLogRepository' = 'import za.ac.alis.legal.persistence.AuditLogRepository'
        'import za.ac.alis.repo.ActRepository' = 'import za.ac.alis.legal.persistence.ActRepository'
        'import za.ac.alis.service.ClientService;' = 'import za.ac.alis.user.service.ClientService;'
        'import za.ac.alis.service.AdminClientService' = 'import za.ac.alis.user.service.AdminClientService'
        'import za.ac.alis.service.LegalPractitionerService' = 'import za.ac.alis.user.service.LegalPractitionerService'
        'import za.ac.alis.service.DealMakerService' = 'import za.ac.alis.user.service.DealMakerService'
        'import za.ac.alis.service.DocumentService' = 'import za.ac.alis.legal.service.DocumentService'
        'import za.ac.alis.service.AiPipelineService' = 'import za.ac.alis.legal.service.AiPipelineService'
        'import za.ac.alis.service.AuditLogService' = 'import za.ac.alis.legal.service.AuditLogService'
        'import za.ac.alis.service.ComplianceService' = 'import za.ac.alis.legal.service.ComplianceService'
        'import za.ac.alis.service.LawRuleService' = 'import za.ac.alis.legal.service.LawRuleService'
        'import za.ac.alis.service.SummaryReportService' = 'import za.ac.alis.legal.service.SummaryReportService'
        'import za.ac.alis.service.PdfReportService' = 'import za.ac.alis.legal.service.PdfReportService'
        'import za.ac.alis.service.AdminDashboardService' = 'import za.ac.alis.legal.service.AdminDashboardService'
        'import za.ac.alis.service.AdminReportService' = 'import za.ac.alis.legal.service.AdminReportService'
        'import za.ac.alis.service.FirebaseStorageService' = 'import za.ac.alis.legal.service.FirebaseStorageService'
        'import za.ac.alis.service.AuditWebSocketService' = 'import za.ac.alis.legal.service.AuditWebSocketService'
        'import za.ac.alis.service.RuleSeederService' = 'import za.ac.alis.legal.service.RuleSeederService'
        'import za.ac.alis.service.AIAnalysisService' = 'import za.ac.alis.ai.service.AIAnalysisService'
        'import za.ac.alis.service.GroqCopilotService' = 'import za.ac.alis.ai.service.GroqCopilotService'
        'import za.ac.alis.service.TextExtractionService' = 'import za.ac.alis.ai.service.TextExtractionService'
        'import za.ac.alis.service.ClauseExtractionService' = 'import za.ac.alis.ai.service.ClauseExtractionService'
        'import za.ac.alis.service.RuleExtractionService' = 'import za.ac.alis.ai.service.RuleExtractionService'
        'import za.ac.alis.security.JwtUtil' = 'import za.ac.alis.auth.security.JwtUtil'
        'import za.ac.alis.security.JwtAuthenticationFilter' = 'import za.ac.alis.auth.security.JwtAuthenticationFilter'
        'import za.ac.alis.config.SecurityConfig' = 'import za.ac.alis.auth.config.SecurityConfig'
        'import za.ac.alis.config.FirebaseConfig' = 'import za.ac.alis.legal.config.FirebaseConfig'
        'import za.ac.alis.webSocket.WebSocketConfig' = 'import za.ac.alis.legal.config.WebSocketConfig'
        'import za.ac.alis.config.AdminSeeder' = 'import za.ac.alis.user.config.AdminSeeder'
        'import za.ac.alis.service.ClientService' = 'import za.ac.alis.user.service.ClientService'
    }
    foreach ($k in $replacements.Keys) {
        $content = $content.Replace($k, $replacements[$k])
    }
    Set-Content -Path $filePath -Value $content -Encoding UTF8 -NoNewline
}

$coreMappings = @(
    @{ folder = "entities"; oldPkg = "za.ac.alis.entities"; newPkg = "za.ac.alis.core.persistence"; sub = "persistence" },
    @{ folder = "enums"; oldPkg = "za.ac.alis.enums"; newPkg = "za.ac.alis.core.enums"; sub = "enums" },
    @{ folder = "dto"; oldPkg = "za.ac.alis.dto"; newPkg = "za.ac.alis.core.dto"; sub = "dto" },
    @{ folder = "projections"; oldPkg = "za.ac.alis.projections"; newPkg = "za.ac.alis.core.projections"; sub = "projections" },
    @{ folder = "queries"; oldPkg = "za.ac.alis.queries"; newPkg = "za.ac.alis.core.queries"; sub = "queries" },
    @{ folder = "utils"; oldPkg = "za.ac.alis.utils"; newPkg = "za.ac.alis.core.util"; sub = "util" }
)
$coreBase = Join-Path $root "alis-core\src\main\java\za\ac\alis\core"
foreach ($m in $coreMappings) {
    $folder = Join-Path $src $m.folder
    if (Test-Path $folder) {
        Get-ChildItem $folder -Filter *.java | ForEach-Object {
            $dest = Join-Path $coreBase "$($m.sub)\$($_.Name)"
            Copy-JavaFile $_.FullName $dest $m.oldPkg $m.newPkg
        }
    }
}

$userFiles = @(
    @{ rel = "repo\ClientRepository.java"; oldPkg = "za.ac.alis.repo"; newPkg = "za.ac.alis.user.persistence" },
    @{ rel = "repo\AdminRepository.java"; oldPkg = "za.ac.alis.repo"; newPkg = "za.ac.alis.user.persistence" },
    @{ rel = "repo\LegalPractitionerRepository.java"; oldPkg = "za.ac.alis.repo"; newPkg = "za.ac.alis.user.persistence" },
    @{ rel = "repo\DealMakerRepository.java"; oldPkg = "za.ac.alis.repo"; newPkg = "za.ac.alis.user.persistence" },
    @{ rel = "service\ClientService.java"; oldPkg = "za.ac.alis.service"; newPkg = "za.ac.alis.user.service" },
    @{ rel = "service\AdminClientService.java"; oldPkg = "za.ac.alis.service"; newPkg = "za.ac.alis.user.service" },
    @{ rel = "service\LegalPractitionerService.java"; oldPkg = "za.ac.alis.service"; newPkg = "za.ac.alis.user.service" },
    @{ rel = "service\DealMakerService.java"; oldPkg = "za.ac.alis.service"; newPkg = "za.ac.alis.user.service" },
    @{ rel = "controller\ClientController.java"; oldPkg = "za.ac.alis.controller"; newPkg = "za.ac.alis.user.controller" },
    @{ rel = "controller\ClientProfileController.java"; oldPkg = "za.ac.alis.controller"; newPkg = "za.ac.alis.user.controller" },
    @{ rel = "controller\LegalPractitionerController.java"; oldPkg = "za.ac.alis.controller"; newPkg = "za.ac.alis.user.controller" },
    @{ rel = "controller\DealmakerController.java"; oldPkg = "za.ac.alis.controller"; newPkg = "za.ac.alis.user.controller" },
    @{ rel = "config\AdminSeeder.java"; oldPkg = "za.ac.alis.config"; newPkg = "za.ac.alis.user.config" }
)
$userBase = Join-Path $root "alis-user\src\main\java"
foreach ($f in $userFiles) {
    $sf = Join-Path $src $f.rel
    $leaf = Split-Path $sf -Leaf
    $sub = $f.newPkg -replace 'za.ac.alis.user.', ''
    $dest = Join-Path $userBase "za\ac\alis\user\$sub\$leaf"
    Copy-JavaFile $sf $dest $f.oldPkg $f.newPkg
}

$authFiles = @(
    @{ rel = "security\JwtUtil.java"; oldPkg = "za.ac.alis.security"; newPkg = "za.ac.alis.auth.security" },
    @{ rel = "security\JwtAuthenticationFilter.java"; oldPkg = "za.ac.alis.security"; newPkg = "za.ac.alis.auth.security" },
    @{ rel = "controller\AuthController.java"; oldPkg = "za.ac.alis.controller"; newPkg = "za.ac.alis.auth.controller" },
    @{ rel = "config\SecurityConfig.java"; oldPkg = "za.ac.alis.config"; newPkg = "za.ac.alis.auth.config" }
)
$authBase = Join-Path $root "alis-auth\src\main\java"
foreach ($f in $authFiles) {
    $sf = Join-Path $src $f.rel
    $leaf = Split-Path $sf -Leaf
    $sub = $f.newPkg -replace 'za.ac.alis.auth.', ''
    $dest = Join-Path $authBase "za\ac\alis\auth\$sub\$leaf"
    Copy-JavaFile $sf $dest $f.oldPkg $f.newPkg
}

$aiFiles = @(
    @{ rel = "service\AIAnalysisService.java"; newPkg = "za.ac.alis.ai.service" },
    @{ rel = "service\GroqCopilotService.java"; newPkg = "za.ac.alis.ai.service" },
    @{ rel = "service\TextExtractionService.java"; newPkg = "za.ac.alis.ai.service" },
    @{ rel = "service\ClauseExtractionService.java"; newPkg = "za.ac.alis.ai.service" },
    @{ rel = "service\RuleExtractionService.java"; newPkg = "za.ac.alis.ai.service" },
    @{ rel = "controller\CopilotController.java"; newPkg = "za.ac.alis.ai.controller" }
)
$aiBase = Join-Path $root "alis-ai\src\main\java"
foreach ($f in $aiFiles) {
    $sf = Join-Path $src $f.rel
    $leaf = Split-Path $sf -Leaf
    $oldPkg = if ($f.rel -like "service*") { "za.ac.alis.service" } else { "za.ac.alis.controller" }
    $sub = $f.newPkg -replace 'za.ac.alis.ai.', ''
    $dest = Join-Path $aiBase "za\ac\alis\ai\$sub\$leaf"
    Copy-JavaFile $sf $dest $oldPkg $f.newPkg
}

$legalRepoFiles = @("DocumentRepository","DocumentContentRepository","FileMetadataRepository","SummaryReportRepository","LawRuleRepository","ClauseRepository","AuditLogRepository","ActRepository")
$legalServiceFiles = @("DocumentService","AiPipelineService","ComplianceService","LawRuleService","AuditLogService","SummaryReportService","PdfReportService","AdminDashboardService","AdminReportService","FirebaseStorageService","AuditWebSocketService","RuleSeederService")
$legalControllerFiles = @("DocumentController","ClientDocumentController","ComplianceController","LawRuleController","ReportController","AuditLogController","AdminDashboardController","TestUploadController")
$legalBase = Join-Path $root "alis-legal\src\main\java"
foreach ($name in $legalRepoFiles) {
    Copy-JavaFile (Join-Path $src "repo\$name.java") (Join-Path $legalBase "za\ac\alis\legal\persistence\$name.java") "za.ac.alis.repo" "za.ac.alis.legal.persistence"
}
foreach ($name in $legalServiceFiles) {
    Copy-JavaFile (Join-Path $src "service\$name.java") (Join-Path $legalBase "za\ac\alis\legal\service\$name.java") "za.ac.alis.service" "za.ac.alis.legal.service"
}
foreach ($name in $legalControllerFiles) {
    Copy-JavaFile (Join-Path $src "controller\$name.java") (Join-Path $legalBase "za\ac\alis\legal\controller\$name.java") "za.ac.alis.controller" "za.ac.alis.legal.controller"
}
Copy-JavaFile (Join-Path $src "config\FirebaseConfig.java") (Join-Path $legalBase "za\ac\alis\legal\config\FirebaseConfig.java") "za.ac.alis.config" "za.ac.alis.legal.config"
Copy-JavaFile (Join-Path $src "webSocket\WebSocketConfig.java") (Join-Path $legalBase "za\ac\alis\legal\config\WebSocketConfig.java") "za.ac.alis.webSocket" "za.ac.alis.legal.config"
if (Test-Path (Join-Path $srcSeed "ActRuleSeeder.java")) {
    Copy-JavaFile (Join-Path $srcSeed "ActRuleSeeder.java") (Join-Path $legalBase "za\ac\alis\legal\seed\ActRuleSeeder.java") "za.ac.seed" "za.ac.alis.legal.seed"
}

$apiBase = Join-Path $root "alis-api\src\main\java\za\ac\alis\api"
Copy-JavaFile (Join-Path $root "src\main\java\za\ac\alis\DemoApplication.java") (Join-Path $apiBase "AlisApplication.java") "za.ac.alis" "za.ac.alis.api"
Copy-JavaFile (Join-Path $src "config\WebConfig.java") (Join-Path $apiBase "config\WebConfig.java") "za.ac.alis.config" "za.ac.alis.api.config"
Copy-JavaFile (Join-Path $src "config\AsyncConfig.java") (Join-Path $apiBase "config\AsyncConfig.java") "za.ac.alis.config" "za.ac.alis.api.config"
Copy-JavaFile (Join-Path $src "config\EnvConfig.java") (Join-Path $apiBase "config\EnvConfig.java") "za.ac.alis.config" "za.ac.alis.api.config"
Copy-JavaFile (Join-Path $src "config\OpenApiConfig.java") (Join-Path $apiBase "config\OpenApiConfig.java") "za.ac.alis.config" "za.ac.alis.api.config"
Copy-JavaFile (Join-Path $root "src\main\java\za\ac\alis\ActiveProfiles.java") (Join-Path $apiBase "ActiveProfiles.java") "za.ac.alis" "za.ac.alis.api"
Copy-JavaFile (Join-Path $src "controller\HealthController.java") (Join-Path $apiBase "controller\HealthController.java") "za.ac.alis.controller" "za.ac.alis.api.controller"

$apiRes = Join-Path $root "alis-api\src\main\resources"
New-Item -ItemType Directory -Path $apiRes -Force | Out-Null
Copy-Item "$srcRes\*" $apiRes -Force

Get-ChildItem -Path $root -Directory -Filter "alis-*" | ForEach-Object {
    Get-ChildItem $_.FullName -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { Apply-ImportMap $_.FullName }
}

Write-Host "Done."

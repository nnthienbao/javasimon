# TODO: Disabled because now we still have some errors in the script (marked by TODO)
# in this script no interaction is possible, so no press any key or Inquire, etc.
#$ErrorActionPreference = "Stop"

Write-Host "Enabling file sharing firewale rules"
netsh advfirewall firewall set rule group="File and Printer Sharing" new enable=yes

# removing Show Task View icon
Set-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced' -Name 'ShowTaskViewButton' -Value 0
Set-WindowsExplorerOptions -EnableShowFileExtensions -EnableShowFullPathInTitleBar
# disabling Cortana
New-Item 'HKCU:\Software\Policies\Microsoft\Windows\Windows Search' -Force | New-ItemProperty -Name 'AllowCortana' -PropertyType 'DWord' -Value 0 -Force | Out-Null
New-Item 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Search' -Force | New-ItemProperty -Name  'SearchboxTaskbarMode' -PropertyType 'DWord' -Value 0 -Force | Out-Null

# Time for tool installation!
cinst -y 7zip.commandline
cinst -y firefox
cinst -y notepad2
cinst -y notepadreplacer -installarguments '/notepad="C:\Progra~1\Notepad2\Notepad2.exe" /verysilent'
cinst -y classic-shell

# Test whether Guest Additions are uploaded
if (Test-Path "C:\Users\vagrant\VBoxGuestAdditions.iso") {
	Write-Host "Installing uploaded Guest Additions"
	certutil -addstore -f "TrustedPublisher" A:\oracle.cer
	Move-Item C:\Users\vagrant\VBoxGuestAdditions.iso C:\Windows\Temp
	7z x C:\Windows\Temp\VBoxGuestAdditions.iso -oC:\Windows\Temp\virtualbox

	Start-Process -FilePath "C:\Windows\Temp\virtualbox\VBoxWindowsAdditions.exe" -ArgumentList "/S" -WorkingDirectory "C:\Windows\Temp\virtualbox" -Wait

	Remove-Item C:\Windows\Temp\virtualbox -Recurse -Force
	Remove-Item C:\Windows\Temp\VBoxGuestAdditions.iso -Force
}

# Test whether Guest Additions are attached (preferred)
if (Test-Path "E:\VBoxWindowsAdditions.exe") {
	Write-Host "Installing attached Guest Additions"
	Start-Process -FilePath "E:\cert\VBoxCertUtil.exe" -ArgumentList @("add-trusted-publisher", "oracle-vbox.cer") -WorkingDirectory "E:\cert" -Wait
	Start-Process -FilePath "E:\VBoxWindowsAdditions.exe" -ArgumentList "/S" -WorkingDirectory "E:\" -Wait
}

# This may take most of the preparation time
Write-Host "Cleaning SxS..."
Dism.exe /online /Cleanup-Image /StartComponentCleanup /ResetBase

# TODO: windir\logs and localappdata\temp cannot be cleaned completely
tasklist /v
@(
	"$env:localappdata\Nuget",
	"$env:localappdata\temp\*",
#	"$env:windir\logs",
	"$env:windir\panther",
	"$env:windir\temp\*",
	"$env:windir\winsxs\manifestcache"
) | % {
		if(Test-Path $_) {
			Write-Host "Removing $_"
			try {
				Takeown /d Y /R /f $_
				Icacls $_ /GRANT:r administrators:F /T /c /q 2>&1 | Out-Null
				Remove-Item $_ -Recurse -Force | Out-Null
			} catch { $global:error.RemoveAt(0) }
		}
	}

Write-Host "defragging..."
Optimize-Volume -DriveLetter C

Write-Host "0ing out empty space..."
$FilePath="c:\zero.tmp"
$Volume = Get-WmiObject win32_logicaldisk -filter "DeviceID='C:'"
$ArraySize= 64kb
$SpaceToLeave= $Volume.Size * 0.05
$FileSize= $Volume.FreeSpace - $SpacetoLeave
$ZeroArray= new-object byte[]($ArraySize)

$Stream= [io.File]::OpenWrite($FilePath)
try {
	$CurFileSize = 0
	while($CurFileSize -lt $FileSize) {
		$Stream.Write($ZeroArray,0, $ZeroArray.Length)
		$CurFileSize +=$ZeroArray.Length
	}
}
finally {
	if($Stream) {
		$Stream.Close()
	}
}

Del $FilePath

Write-Host "copying auto unattend file"
mkdir C:\Windows\setup\scripts
copy-item a:\SetupComplete.cmd C:\Windows\setup\scripts\SetupComplete.cmd -Force

mkdir C:\Windows\Panther\Unattend
copy-item a:\postunattend.xml C:\Windows\Panther\Unattend\unattend.xml

Write-Host "Recreate pagefile after sysprep"
$System = GWMI Win32_ComputerSystem -EnableAllPrivileges
if ($system -ne $null) {
	$System.AutomaticManagedPagefile = $true
	$System.Put()
}

Write-Host 'Provision script FINISHED';
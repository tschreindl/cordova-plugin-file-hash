/* global Windows, WinJS, MSApp */

var utils = require('cordova/utils');

var getFileFromPathAsync = Windows.Storage.StorageFile.getFileFromPathAsync;


function cordovaPathToNative(path) {
    // turn / into \\
    var cleanPath = path.replace(/\//g, '\\');
    // turn  \\ into \
    cleanPath = cleanPath.replace(/\\+/g, '\\');
    return cleanPath;
}

function nativePathToCordova (path) {
    var cleanPath = path.replace(/\\/g, '/');
    return cleanPath;
}

var driveRE = new RegExp("^[/]*([A-Z]:)");

var WinFS = function (name, root) {
    this.winpath = root.winpath;
    if (this.winpath && !/\/$/.test(this.winpath)) {
        this.winpath += '/';
    }
    root.fullPath = '/';
    if (!root.nativeURL) { root.nativeURL = 'file://' + sanitize(this.winpath + root.fullPath).replace(':', '%3A'); }
    WinFS.__super__.constructor.call(this, name, root);
};

utils.extend(WinFS, FileSystem);

WinFS.prototype.__format__ = function (fullPath) {
    var path = sanitize('/' + this.name + (fullPath[0] === '/' ? '' : '/') + FileSystem.encodeURIPath(fullPath));
    return 'cdvfile://localhost' + path;
};

var AllFileSystems;

function getAllFS () {
    if (!AllFileSystems) {
        AllFileSystems = {
            'persistent':
            Object.freeze(new WinFS('persistent', {
                name: 'persistent',
                nativeURL: 'ms-appdata:///local',
                winpath: nativePathToCordova(Windows.Storage.ApplicationData.current.localFolder.path)
            })),
            'temporary':
            Object.freeze(new WinFS('temporary', {
                name: 'temporary',
                nativeURL: 'ms-appdata:///temp',
                winpath: nativePathToCordova(Windows.Storage.ApplicationData.current.temporaryFolder.path)
            })),
            'application':
            Object.freeze(new WinFS('application', {
                name: 'application',
                nativeURL: 'ms-appx:///',
                winpath: nativePathToCordova(Windows.ApplicationModel.Package.current.installedLocation.path)
            })),
            'root':
            Object.freeze(new WinFS('root', {
                name: 'root',
                // nativeURL: 'file:///'
                winpath: ''
            }))
        };
    }
    return AllFileSystems;
}

function sanitize(path) {
    var slashesRE = new RegExp('/{2,}','g');
    var components = path.replace(slashesRE, '/').split(/\/+/);
    // Remove double dots, use old school array iteration instead of RegExp
    // since it is impossible to debug them
    for (var index = 0; index < components.length; ++index) {
        if (components[index] === "..") {
            components.splice(index, 1);
            if (index > 0) {
                // if we're not in the start of array then remove preceeding path component,
                // In case if relative path points above the root directory, just ignore double dots
                // See file.spec.111 should not traverse above above the root directory for test case
                components.splice(index-1, 1);
                --index;
            }
        }
    }
    return components.join('/');
}

function getFilesystemFromPath (path) {
    var res;
    var allfs = getAllFS();
    Object.keys(allfs).some(function (fsn) {
        var fs = allfs[fsn];
        if (path.indexOf(fs.winpath) === 0) { res = fs; }
        return res;
    });
    return res;
}


var msapplhRE = new RegExp('^ms-appdata://localhost/');
function pathFromURL(url) {
    url=url.replace(msapplhRE,'ms-appdata:///');
    var path = decodeURIComponent(url);
    // support for file name with parameters
    if (/\?/g.test(path)) {
        path = String(path).split("?")[0];
    }
    if (path.indexOf("file:/")===0) {
        if (path.indexOf("file://") !== 0) {
            url = "file:///" + url.substr(6);
        }
    }
    
    ['file://','ms-appdata:///','cdvfile://localhost/'].every(function(p) {
        if (path.indexOf(p)!==0)
            return true;
        var thirdSlash = path.indexOf("/", p.length);
        if (thirdSlash < 0) {
            path = "";
        } else {
            path = sanitize(path.substr(thirdSlash));
        }
    });
    
    return path.replace(driveRE,'$1');
}

function getFilesystemFromURL (url) {
    url = url.replace(msapplhRE, 'ms-appdata:///');
    var res;
    if (url.indexOf('file:/') === 0) { res = getFilesystemFromPath(pathFromURL(url)); } else {
        var allfs = getAllFS();
        Object.keys(allfs).every(function (fsn) {
            var fs = allfs[fsn];
            if (url.indexOf(fs.root.nativeURL) === 0 ||
                url.indexOf('cdvfile://localhost/' + fs.name + '/') === 0) {
                res = fs;
                return false;
            }
            return true;
        });
    }
    return res;
}

function makeHash(win, fail, url, algo, hashObj) {
    var fs = getFilesystemFromURL(url);
    var path = pathFromURL(url);
    if (!fs) {
        fail({code: 3, message: "File access error"});
        return;
    }
    var wpath = cordovaPathToNative(sanitize(fs.winpath + path));

    getFileFromPathAsync(wpath).then(
        function (storageFile) {
            Windows.Storage.FileIO.readBufferAsync(storageFile).done(
                function (buffer) {
                    hashObj.append(buffer)
                    var hashText = Windows.Security.Cryptography.CryptographicBuffer.encodeToHexString(hashObj.getValueAndReset());
                    win({file: url, algo: algo, result: hashText});
                }
            );
        }, function () {
        fail({code: 2, message: "File not found"});
    }
    );
}



module.exports = {
    
    md2: function (win, fail, args){
        fail({code: 5, message: "Algorithm not implemented."});
    },
    
    md5: function (win, fail, args){
        var url = args[0];
        var alg = Windows.Security.Cryptography.Core.HashAlgorithmProvider.openAlgorithm(Windows.Security.Cryptography.Core.HashAlgorithmNames.md5);
        var hash = alg.createHash();
        makeHash(win, fail, url, "md5", hash);
    },
    
    sha1: function (win, fail, args){
        var url = args[0];
        var alg = Windows.Security.Cryptography.Core.HashAlgorithmProvider.openAlgorithm(Windows.Security.Cryptography.Core.HashAlgorithmNames.sha1);
        var hash = alg.createHash();
        makeHash(win, fail, url, "sha1", hash);
    },
    
    sha256: function (win, fail, args){
        var url = args[0];
        var alg = Windows.Security.Cryptography.Core.HashAlgorithmProvider.openAlgorithm(Windows.Security.Cryptography.Core.HashAlgorithmNames.sha256);
        var hash = alg.createHash();
        makeHash(win, fail, url, "sha256", hash);
    },
    
    sha384: function (win, fail, args){
        var url = args[0];
        var alg = Windows.Security.Cryptography.Core.HashAlgorithmProvider.openAlgorithm(Windows.Security.Cryptography.Core.HashAlgorithmNames.sha384);
        var hash = alg.createHash();
        makeHash(win, fail, url, "sha384", hash);
    },
    
    sha512: function (win, fail, args){
        var url = args[0];
        var alg = Windows.Security.Cryptography.Core.HashAlgorithmProvider.openAlgorithm(Windows.Security.Cryptography.Core.HashAlgorithmNames.sha512);
        var hash = alg.createHash();
        makeHash(win, fail, url, "sha512", hash);
    },
};

require("cordova/exec/proxy").add("FileHash",module.exports);

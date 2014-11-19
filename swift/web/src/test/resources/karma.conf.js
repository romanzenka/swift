module.exports = function (config) {
    config.set({
        basePath: '../../../',
        frameworks: ['jasmine'],
        files: [
            'src/main/webapp/report/**/*.js',
            'src/test/js/**/*.js'
        ],
        exclude: [],
        preprocessors: {
            'src/main/webapp/report/**/*.js': ['coverage']
        },
        reporters: ['progress', 'coverage'],
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: true,
        browsers: ['Chrome', 'PhantomJS'],
        singleRun: false,
        plugins: [
            'karma-jasmine',
            'karma-chrome-launcher',
            'karma-phantomjs-launcher',
            'karma-junit-reporter',
            'karma-coverage'
        ],
        coverageReporter: {
            type: 'html',
            dir: 'target/coverage/'
        }
    });
};
// Gruntfile with the configuration of grunt-express and grunt-open. No livereload yet!
module.exports = function(grunt) {
 
  // Load Grunt tasks declared in the package.json file
  require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);
 
  // Configure Grunt 
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    concat: {
      dist: {
        files: {
          'build/js/fim.js': [
            'bin/js/mgj_im.js'
            ],
          'build/css/styles.css': [
            'bin/css/**/*.css'
          ],
          'build/js/lib.js': [
            'js/**/*.js'
          ]
        }
      }
    },
    copy: {
      dist: {
        files: [{
          src: 'index.css',
          dest: 'build/index.css'
        },{
          src: 'data/**/*',
          dest: 'build/'
        },{
          src: 'out/**/*',
          dest: 'build/'
        }]
      }
    },
    processhtml: {
      dist: {
        options: {
          process: true,
          data: {
            title: 'mgj-im',
            message: 'A game made for the Monser Game Jam in Brussels using the theme \'inner mechanic\'.'
          }
        },
        files: {
          'build/index.html': ['index.html']
        }
      }
    },
    uglify: {
      options: {
        report: 'min',
        preserveComments: 'some'
      },
      dist: {
        files: {
          'build/js/mgj_im_prod.js': [
            'build/js/mgj_im_prod_min.js'
          ]
        }
      }
    },
    // grunt-express will serve the files from the folders listed in `bases`
    // on specified `port` and `hostname`
    express: {
      all: {
        options: {
          port: 8000,
          hostname: "0.0.0.0",
          bases: ["build"], // Replace with the directory you want the files served from
                              // Make sure you don't use `.` or `..` in the path as Express
                              // is likely to return 403 Forbidden responses if you do
                              // http://stackoverflow.com/questions/14594121/express-res-sendfile-throwing-forbidden-error
          livereload: true
        }
      }
    },
    // grunt-watch will monitor the projects files
    watch: {
      all: {
        // Replace with whatever file you want to trigger the update from
        // Either as a String for a single entry 
        // or an Array of String for multiple entries
        // You can use globing patterns like `css/**/*.css`
        // See https://github.com/gruntjs/grunt-contrib-watch#files
        files: ['bin/js/mgj_im.js', 'data/**/*.png', 'index.html', 'src/sass/**/*.scss'],
        tasks: ['sass', 'concat',/*'uglify',*/ 'copy', 'processhtml']
      }
    },
    sass: {
      dist: {
        files: [{
          expand: true,
          cwd: 'src/sass',
          src: ['*.scss'],
          dest: 'bin/css',
          ext: '.css'
        }]
      }
    }
  });
 
  // Creates the `server` task
  grunt.registerTask('server', [
    'sass', 'concat',/*'uglify',*/ 'copy', 'processhtml',
    'express',
    'watch'
  ]);
};
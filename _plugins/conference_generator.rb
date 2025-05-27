require 'jekyll'
require 'fileutils'

module Jekyll
  class ConferenceGenerator < Generator
    safe true

    def generate(site)
      puts "---------- ConferenceGenerator started ----------"
       if (!defined?@render_count)
          @render_count = 1
       end

       if @render_count < 1
         Jekyll.logger.info('already fetched data')
         return
       end

       conferencesFilesKey = site.data['conferences'].keys.select { |key| key.include?('conferences') }
       conferencesFilesKey.each do |key|
         Jekyll.logger.info("Processing #{key}")

          conferences = site.data['conferences'][key]

          if conferences
            conferences.each do |conference_id, details|
              post_path = File.join('_posts', "#{details['post-date']}-#{conference_id}-talks.markdown")

              post_data = {
                'layout' => 'conference',
                'title' => "Talks donnés à #{details['name']}",
                'date' => details['post-date'],
                'permalink' => "/talks/#{details['talks-url']}",
                'excerpt' => details['excerpt'],
                'categories' => "#{details['categories']}",
                'tags' => "\n - #{details['tags']}",
                'conference-name' => conference_id
              }
              
              post_content = "---\n"
              post_content += post_data.map { |key, value| "#{key}: #{value}" }.join("\n") + "\n"
              post_content += "---\n"

              # Write the post file
              FileUtils.mkdir_p(File.dirname(post_path))
              File.write(post_path, post_content)

              puts "Generated post: #{post_path}"
            end
          else
            puts "No conferences data found in site.data"
          end
          Jekyll.logger.info('Data fetched successfully.')
          @render_count = @render_count - 1
          puts "---------- ConferenceGenerator finished ----------"
        end
      end
  end
end
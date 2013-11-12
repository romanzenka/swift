package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.swift.webservice.diff.IdCounts;
import edu.mayo.mprc.swift.webservice.diff.InputFile;
import edu.mayo.mprc.swift.webservice.diff.SearchDiff;
import edu.mayo.mprc.swift.webservice.diff.SwiftSearch;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Controller
public final class SearchDiffs {
	@Resource(name = "swiftDao")
	private SwiftDao swiftDao;

	@Resource(name = "searchDbDao")
	private SearchDbDao searchDbDao;

	@Resource(name = "webUiHolder")
	private WebUiHolder webUiHolder;

	public SearchDiffs() {
	}

	@RequestMapping(value = "/search-diffs", method = RequestMethod.GET)
	public ModelAndView searchDiff(@RequestParam("id") final int swiftSearchId) {
		final SwiftDao dao = getSwiftDao();
		try {
			dao.begin();
			final SearchRun searchRun = dao.getSearchRunForId(swiftSearchId);
			if (searchRun.getSwiftSearch() == null) {
				throw new MprcException("This search is not defined in the database");
			}
			final SwiftSearchDefinition swiftSearchDefinition = dao.getSwiftSearchDefinition(searchRun.getSwiftSearch());

			// Translate the FileSearch objects into lightweight ones to be output
			final List<FileSearch> files = swiftSearchDefinition.getInputFiles();
			int inputFileId = 0;
			final List<InputFile> inputFiles = new ArrayList<InputFile>(files.size());
			for (final FileSearch file : files) {
				inputFileId++;
				final InputFile fileOutput = new InputFile(inputFileId, file);
				inputFiles.add(fileOutput);
			}

			// Translate the SearchRun objects into lightweight ones to be output
			final List<SearchRun> runs = dao.findSearchRunsForFiles(files);
			final List<SwiftSearch> searches = new ArrayList<SwiftSearch>();
			for (final SearchRun run : runs) {
				final SwiftSearchDefinition definition = dao.getSwiftSearchDefinition(run.getSwiftSearch());
				final SwiftSearch runOutput = new SwiftSearch(run, definition, getWebUiHolder().getWebUi());
				searches.add(runOutput);

				// Add newly discovered search files
				for (final FileSearch search : definition.getInputFiles()) {
					boolean alreadyThere = false;
					for (final InputFile inputFile : inputFiles) {
						if (inputFile.getPath().equalsIgnoreCase(search.getInputFile().getPath())) {
							alreadyThere = true;
							break;
						}
					}
					if (!alreadyThere) {
						inputFileId++;
						inputFiles.add(new InputFile(inputFileId, search));
					}
				}
			}

			// Fill in the map
			final Map<Integer, Map<Integer, IdCounts>> map = new HashMap<Integer, Map<Integer, IdCounts>>();

			for (final InputFile file : inputFiles) {
				for (final SearchRun run : runs) {
					Map<Integer, IdCounts> subMap = map.get(file.getId());
					if (subMap == null) {
						subMap = new HashMap<Integer, IdCounts>();
						map.put(file.getId(), subMap);
					}

					final boolean fileUtilized = dao.isFileInSearchRun(file.getPath(), run);
					final Integer proteinGroups = fileUtilized ? searchDbDao.getScaffoldProteinGroupCount(file.getPath(), run.getReports()) : 0;
					final IdCounts value = new IdCounts(fileUtilized, proteinGroups.intValue());
					subMap.put(run.getId(), value);
				}
			}

			final SearchDiff searchDiff = new SearchDiff(map, searches, inputFiles);

			dao.commit();

			final ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("diffs", searchDiff);
			return modelAndView;
		} catch (Exception t) {
			dao.rollback();
			throw new MprcException("Could not produce search diff", t);
		}
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}

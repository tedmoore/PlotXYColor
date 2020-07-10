/*
Ted Moore
www.tedmooremusic.com
ted@tedmooremusic.com
June 4, 2020
*/

PlotXYColor {
	var	axisFeatureIndex, // dictionary [string of axis -> vector index]
	axisOptions, // array of strings that are the labels of the vector indices (columns)
	axisPums, // pop up menus for selecting what column belongs to what axis
	circleRadius = 6, // how big the dots are
	corpus, // the data that is passed in, but not that data that get's used in the course of things, that's prCorpus
	corpus_dims,
	colorArray,
	disp_colors,
	headerArray, // array of strings that user can pass for column headers (OPTIONAL)
	idArray, // array of *anything* (ints, strings, whatever), that the user can pass to be returned on "hover over" (OPTIONAL)
	ignorePrevious, // when the same data point is selected twice in a row, should it be reported twice, or not? (DEFAULT = true)
	lastHovered = nil, // stores the last point that was hovered over
	mouseOverFunc, /* user passed function of what to do when the mouse hovers over a point
	---------------passed to this function are:
	(0) index of data point (unless idArray is passed in on initialization, in which case the data point's id is passed)
	*/
	plotView, // the subview where everything is plotted
	plotWin, // the window
	prCorpus, // a private array of objects that handles the corpus data
	slewTime = 0.5; // how long it takes for the dots to move between different spots in the plot

	*new {
		arg corpus, mouseOverFunc, headerArray /* optional */, idArray /* optional */, colorArray /* optional */, slewTime = 0.5, ignorePrevious = true;
		^super.new.init(corpus,mouseOverFunc,headerArray,idArray,colorArray,slewTime,ignorePrevious);
	}

	init {
		arg corpus_, mouseOverFunc_, headerArray_, idArray_, colorArray_, slewTime_, ignorePrevious_;
		colorArray = colorArray_;
		corpus = corpus_;
		corpus_dims = corpus[0].size;

		if(corpus_dims < 2,{
			"Corpus must be at least 2 dimensions".throw;
		});

		if((corpus_dims < 3).or(colorArray.notNil),{
			disp_colors = false;
		},{
			disp_colors = true;
		});

		mouseOverFunc = mouseOverFunc_;
		headerArray = headerArray_;
		idArray = idArray_;
		slewTime = slewTime_;
		ignorePrevious = ignorePrevious_;

		// if no header information is passed, make header labels "Feature n"
		if(headerArray.notNil,{
			axisOptions = headerArray;
		},{
			axisOptions = corpus[0].size.collect({
				arg i;
				"Feature %".format(i);
			});
		});

		this.createPlotWindow;
	}

	createPlotWindow {
		var container;
		plotWin = Window("Plot",Rect(0,0,1200,900))
		.acceptsMouseOver_(true);
		plotWin.view.onResize_({
			plotView.bounds_(Rect(0,20,plotWin.view.bounds.width,plotWin.view.bounds.height-20));
			this.slewDisplay(0);
		});

		// this is just a sub plot for putting the drop down menus in
		container = CompositeView(plotWin,Rect(0,0,plotWin.view.bounds.width,20))
		.background_(Color.white);
		container.decorator_(FlowLayout(container.bounds,0@0,0@0));

		// dictionary lookup (name of axis -> what vector index it is currently displaying)
		axisFeatureIndex = Dictionary.new;

		// make the drop down menus
		axisPums = ["X Axis","Y Axis","Color"].collect({
			arg name, i;
			var pum = nil;

			if(i < corpus_dims,{
				// start with the axis names as displaying columns 0, 1, 2
				axisFeatureIndex.put(name,min(i,corpus_dims-1));

				// make this drop down menu
				StaticText(container,Rect(0,0,50,20)).string_(" " + name);
				pum = PopUpMenu(container,Rect(0,0,160,20))
				.items_(axisOptions) // it has the drop down options made above
				.action_({
					arg pum;
					// when something is selected, that index is set in the dictionary to the name of this axis
					axisFeatureIndex.put(name,pum.value);
					this.slewDisplay(slewTime); // update the display
				})
				.value_(i); // start it off as 0, 1, or 2 (respectively)
			});

			pum; // return the menu to be part of the axisPums array
		});

		plotView = UserView(plotWin,Rect(0,20,plotWin.view.bounds.width,plotWin.view.bounds.height-20))
		.drawFunc_({ // this is the "draw loop" for a supercollider view - its actually only called though when it needs to be updated
			// i.e. it's not actually looping. this runs everytime plotView.refresh is called.

			prCorpus.do({ // go through the entire private corpus and put a dot on the screen for each
				arg corpusItem, i;
				Pen.addOval(corpusItem.dispRect);
				if(colorArray.isNil,{
					if(corpus_dims > 2,{
						Pen.color_(Color.hsv(corpusItem.color,1,1));
					},{
						Pen.color_(Color.black);
					});
				},{
					Pen.color_(colorArray[i]);
				});
				Pen.draw;
			});
		})
		.mouseOverAction_({ // this function gets called each time the mouse moves over the window
			arg view, px, py, modifiers;
			var mousePoint = Point(px,py);
			prCorpus.do({ // go through the whole corpus...
				arg corpusItem, i;

				if(corpusItem.dispRect.notNil,{

					if(corpusItem.dispRect.contains(mousePoint),{ // if the mouse is inside this datapoint's dot...
						this.returnIndex(i,px,py); // return the index
					});
				});
			});
		})
		.mouseMoveAction_({ // if the mouse button is down and the mouse moves over the window this function is called
			arg view, x, y, modifiers;
			//["mouse move",view, x, y, modifiers].postln;
			this.findClosest(x,y); // find the closest point...
		});

		// =============== before we display the window and start using, make the private corpus =============
		prCorpus = corpus.collect({
			arg vector;
			var xindex, yindex, colorindex, dispx, dispy, color;

			// get the vector indicies that are currently assiged to the three axes (here it will obviously be 0, 1, 2)
			# xindex, yindex, colorindex = this.getCurrentIndices;

			// using the axes indices, get the appropriately scaled values for display x pos, display y pos, and display color for this vector
			# dispx, dispy, color = this.getScaledXYColorFromIndices(xindex,yindex,colorindex,vector);

			// each private corpus item has the vector, but also keeps track of where on the screen and what color its dot is
			(vector:vector,dispRect:Rect(dispx,dispy,circleRadius,circleRadius),color:color);
		});

		// update the display stuff
		this.slewDisplay(0);

		// show the window
		plotWin.front;
	}

	getrxry { // pass in an x, y point from the screen (in pixels measurements) and get returned the normalized x, y (0 to 1)
		arg px, py;
		var rx = px.linlin(0,plotView.bounds.width,0,1);
		var ry = py.linlin(0,plotView.bounds.height,1,0); // y is inverted for display purposes
		^[rx,ry];
	}

	returnIndex { // this gets called whenever something is going to be passed to the user in teh "mouseOverFunc"
		arg idx,px,py; // pass in the index of the data point to be returned and the x, and y of that data point in pixels

		// if ignore previous == true, check to make sure this index isn't the most recent one. if it is, don't pass it again
		if((idx != lastHovered).or(ignorePrevious.not),{
			var rx, ry, xindex, yindex, colorindex;

			lastHovered = idx; // set "previous" to be this index

			# rx, ry = this.getrxry(px,py); // pass in pixel x,y to get normalized x,y

			# xindex, yindex, colorindex = this.getCurrentIndices; // what are the current vector indicies that are being displayed

			// if the user passed in an idArray, don't pass the data point's index, pass the data point's id from that idArray
			if(idArray.notNil,{idx = idArray[idx]});

			/* evaluate the function the user passed. pass to that function:
			(0) the index (or id) of the point that was hovered over
			(1) the normalized x position of the mouse
			(2) the normalized y position of the mouse
			(3) the current vector index (i.e., feature) that is displayed on the x axis
			(4) the current vector index (i.e., feature) that is displayed on the y axis
			*/
			mouseOverFunc.value(idx,rx,ry,xindex,yindex);

		});
	}

	findClosest {
		arg x, y;
		var mousePt = Point(x,y);

		var record_dist = inf;
		var winner = nil;
		prCorpus.do({
			arg corpusItem, i;
			var dist = corpusItem.dispRect.origin.dist(mousePt);
			if(dist < record_dist,{
				record_dist = dist;
				winner = i;
			});
		});
		this.returnIndex(winner,x,y);
	}

	getCurrentIndices {
		var xindex = axisFeatureIndex.at("X Axis");
		var yindex = axisFeatureIndex.at("Y Axis");
		var colorindex = axisFeatureIndex.at("Color");
		^[xindex,yindex,colorindex];
	}

	getScaledXYColorFromIndices { // pass in what vector indices are currently being displayed and a vector and get back the appropriately scaled values
		arg xindex, yindex, colorindex, vector;
		var dispx = vector[xindex].linlin(0,1,0,plotView.bounds.width-circleRadius);
		var dispy = vector[yindex].linlin(0,1,plotView.bounds.height-circleRadius,0);
		var color = nil;
		if(colorindex.notNil,{
			color = vector[colorindex].linlin(0,1,0.8,0);// because both 0 and 1 are red...
		});
		^[dispx,dispy,color];
	}

	slewDisplay {
		arg time = 0.1; // how long should the "slew" take
		time = max(time,0.1);
		Task({
			var startLocs = List.new; // where all the points are starting from (where they are right now)
			var endPts = List.new; // where they will be ending up after they slew
			var startColors = List.new; // what color the points are right now
			var endColors = List.new; // what color they will be after the transition
			var updateTime = 30.reciprocal; // reciprocal of the frame rate for the animation
			var n_ = time / updateTime; // how many frames of animation will it take to complete this transition
			var currentIndices = this.getCurrentIndices; // what are the currently display indices (the ones that the user must have just changed to

			prCorpus.do({ // go through each data point
				arg corpusItem;
				var endx, endy, endcolor;
				var color_index = min(2,corpus_dims - 1);

				startLocs.add(corpusItem.dispRect.copy); // add to this list where this data point is currently (where it will be starting from)
				startColors.add(corpusItem.color); // add to this list what color this data point is currently (where it will be starting from)

				# endx, endy, endcolor = this.getScaledXYColorFromIndices( // get the values that this point will be ending up at
					currentIndices[0], // x
					currentIndices[1], // y
					currentIndices[color_index], // color
					corpusItem.vector
				);

				endPts.add(Point(endx,endy)); // add to this list where the data point will end its journey
				endColors.add(endcolor); // add to this list the color that the data point will end its journey as
			});

			n_.do({ // do n_ many frames
				arg i;
				var lerp = i.linlin(0,n_-1,-pi,0).cos.linlin(-1,1,0,1); // given i, how far along in the interpolation is the animation
				prCorpus.do({ // go through each corpus item
					arg corpusItem, i;
					var ix = lerp.linlin(0,1,startLocs[i].left,endPts[i].x); // given the interpolation amount, what is x
					var iy = lerp.linlin(0,1,startLocs[i].top,endPts[i].y); // given the interpolation amount, what is y
					corpusItem.dispRect = Rect(ix,iy,circleRadius,circleRadius); // set this data point's display info to interplation's x,y
					if(corpus_dims > 2,{
						corpusItem.color = lerp.linlin(0,1,startColors[i],endColors[i]); // set this data point's color to interpolation color
					});
				});

				// update display
				plotView.refresh;
				// wait some amount of time before running next animation frame
				updateTime.wait;
			});

		},AppClock).play;
	}
}